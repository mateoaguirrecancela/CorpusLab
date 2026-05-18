package es.udc.fic.corpuslab.modules.project.dataset;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import es.udc.fic.corpuslab.common.redis.RedisStreamService;
import es.udc.fic.corpuslab.modules.project.dataset.dtos.ProjectDatasetUploadEventDto;
import es.udc.fic.corpuslab.modules.project.dataset.dtos.UploadProjectDatasetResponseDto;

@Service
public class ProjectDatasetUploadWorker {

    private static final Logger logger = LoggerFactory.getLogger(ProjectDatasetUploadWorker.class);
    private static final String CONSUMER_NAME = "corpuslab-worker";

    private final RedisStreamService redisStreams;
    private final ProjectDatasetItemService projectDatasetItemService;
    private final ProjectDatasetUploadQueue uploadQueue;
    private final ProjectDatasetUploadEvents events;

    public ProjectDatasetUploadWorker(
            RedisStreamService redisStreams,
            ProjectDatasetItemService projectDatasetItemService,
            ProjectDatasetUploadQueue uploadQueue,
            ProjectDatasetUploadEvents events) {
        this.redisStreams = redisStreams;
        this.projectDatasetItemService = projectDatasetItemService;
        this.uploadQueue = uploadQueue;
        this.events = events;
    }

    @Scheduled(fixedDelayString = "${app.dataset-upload.worker-delay-ms:1000}")
    public void processNextUploadJob() {
        List<MapRecord<String, Object, Object>> records;
        try {
            records = readPendingThenNew();
        } catch (RuntimeException ex) {
            logger.error("Dataset upload worker could not read from Redis stream {}", ProjectDatasetUploadQueue.STREAM_KEY, ex);
            return;
        }

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            processRecord(record);
        }
    }

    private List<MapRecord<String, Object, Object>> readPendingThenNew() {
        return redisStreams.readPendingThenNew(
                ProjectDatasetUploadQueue.STREAM_KEY,
                ProjectDatasetUploadQueue.GROUP,
                CONSUMER_NAME,
                1);
    }

    private void processRecord(MapRecord<String, Object, Object> record) {
        Map<Object, Object> rawMessage = record.getValue();
        DatasetUploadJobMetadata metadata;
        try {
            metadata = parseJobMetadata(rawMessage);
        } catch (RuntimeException ex) {
            logger.error("Discarding invalid dataset upload job {}", record.getId(), ex);
            acknowledge(record);
            return;
        }

        DatasetUploadJobMessage jobMessage;
        try {
            jobMessage = parseJobMessage(metadata, rawMessage);
        } catch (RuntimeException ex) {
            logger.error("Discarding invalid dataset upload job {}", record.getId(), ex);
            events.publish(new ProjectDatasetUploadEventDto(
                    metadata.jobId(), "FAILED", 100, ex.getMessage(), null),
                    metadata.authenticatedEmail(), metadata.projectId());
            acknowledge(record);
            return;
        }

        try {
            events.publish(new ProjectDatasetUploadEventDto(
                    jobMessage.jobId(), "RUNNING", 20, "Processing dataset files", null),
                    jobMessage.authenticatedEmail(), jobMessage.projectId());

            UploadProjectDatasetResponseDto result = projectDatasetItemService.uploadDataset(
                    jobMessage.authenticatedEmail(),
                    jobMessage.researchGroupId(),
                    jobMessage.projectId(),
                    jobMessage.stagedFiles().stream().<MultipartFile>map(StagedMultipartFile::new).toList());

            events.publish(new ProjectDatasetUploadEventDto(
                    jobMessage.jobId(), "COMPLETED", 100, "Dataset upload completed", result),
                    jobMessage.authenticatedEmail(), jobMessage.projectId());
            acknowledge(record);
        } catch (RuntimeException ex) {
            logger.error("Dataset upload job {} failed", jobMessage.jobId(), ex);
            events.publish(new ProjectDatasetUploadEventDto(
                    jobMessage.jobId(), "FAILED", 100, ex.getMessage(), null),
                    jobMessage.authenticatedEmail(), jobMessage.projectId());
            acknowledge(record);
        } finally {
            cleanup(jobMessage.stagedFiles());
        }
    }

    private DatasetUploadJobMetadata parseJobMetadata(Map<Object, Object> message) {
        String jobId = requiredValue(message, "jobId");
        String authenticatedEmail = requiredValue(message, "authenticatedEmail");
        Long researchGroupId = longValue(message, "researchGroupId");
        Long projectId = longValue(message, "projectId");
        return new DatasetUploadJobMetadata(jobId, authenticatedEmail, researchGroupId, projectId);
    }

    private DatasetUploadJobMessage parseJobMessage(DatasetUploadJobMetadata metadata, Map<Object, Object> message) {
        List<ProjectDatasetUploadQueue.StagedDatasetFile> stagedFiles = uploadQueue.readStagedFiles(message);
        return new DatasetUploadJobMessage(
                metadata.jobId(),
                metadata.authenticatedEmail(),
                metadata.researchGroupId(),
                metadata.projectId(),
                stagedFiles);
    }

    private void acknowledge(MapRecord<String, Object, Object> record) {
        redisStreams.acknowledge(ProjectDatasetUploadQueue.STREAM_KEY,
                ProjectDatasetUploadQueue.GROUP, record.getId());
    }

    private void cleanup(List<ProjectDatasetUploadQueue.StagedDatasetFile> stagedFiles) {
        for (ProjectDatasetUploadQueue.StagedDatasetFile stagedFile : stagedFiles) {
            Path jobDirectory;
            try {
                jobDirectory = uploadQueue.resolveStagedJobDirectory(stagedFile.path());
            } catch (RuntimeException ex) {
                logger.warn("Skipping invalid staged dataset path during cleanup", ex);
                continue;
            }

            if (!Files.exists(jobDirectory)) {
                continue;
            }
            try (var walk = Files.walk(jobDirectory)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(file -> {
                            try {
                                Files.deleteIfExists(file);
                            } catch (IOException ignored) {
                                // Best effort cleanup; stale staged files can be purged by ops.
                            }
                        });
            } catch (IOException ignored) {
                // Best effort cleanup.
            }
        }
    }

    private Long longValue(Map<Object, Object> message, String key) {
        try {
            return Long.valueOf(requiredValue(message, key));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid dataset upload job field: " + key, ex);
        }
    }

    private String requiredValue(Map<Object, Object> message, String key) {
        Object value = message.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("Missing dataset upload job field: " + key);
        }
        return String.valueOf(value);
    }

    private record DatasetUploadJobMessage(
            String jobId,
            String authenticatedEmail,
            Long researchGroupId,
            Long projectId,
            List<ProjectDatasetUploadQueue.StagedDatasetFile> stagedFiles) {
    }

    private record DatasetUploadJobMetadata(
            String jobId,
            String authenticatedEmail,
            Long researchGroupId,
            Long projectId) {
    }

    private final class StagedMultipartFile implements MultipartFile {
        private final ProjectDatasetUploadQueue.StagedDatasetFile stagedFile;

        private StagedMultipartFile(ProjectDatasetUploadQueue.StagedDatasetFile stagedFile) {
            this.stagedFile = stagedFile;
        }

        @Override
        public String getName() {
            return stagedFile.parameterName();
        }

        @Override
        public String getOriginalFilename() {
            return stagedFile.originalFilename();
        }

        @Override
        public String getContentType() {
            return stagedFile.contentType();
        }

        @Override
        public boolean isEmpty() {
            try {
                return Files.size(path()) == 0L;
            } catch (IOException ex) {
                return true;
            }
        }

        @Override
        public long getSize() {
            try {
                return Files.size(path());
            } catch (IOException ex) {
                return 0L;
            }
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(path());
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(path());
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.copy(path(), dest.toPath());
        }

        private Path path() {
            return uploadQueue.resolveStagedPath(stagedFile.path());
        }
    }
}
