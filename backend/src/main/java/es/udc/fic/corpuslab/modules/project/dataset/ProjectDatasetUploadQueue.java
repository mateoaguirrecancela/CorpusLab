package es.udc.fic.corpuslab.modules.project.dataset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.udc.fic.corpuslab.common.redis.RedisStreamService;
import es.udc.fic.corpuslab.modules.project.dataset.dtos.ProjectDatasetUploadEventDto;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectDatasetException;

@Service
public class ProjectDatasetUploadQueue {

    static final String STREAM_KEY = "corpuslab:dataset-upload";
    static final String GROUP = "dataset-upload-workers";
    private static final String FALLBACK_FILE_NAME = "dataset-file";

    private final RedisStreamService redisStreams;
    private final ObjectMapper objectMapper;
    private final ProjectDatasetUploadEvents events;
    private final Path stagingRoot;

    public ProjectDatasetUploadQueue(
            RedisStreamService redisStreams,
            ObjectMapper objectMapper,
            ProjectDatasetUploadEvents events,
            @Value("${app.dataset-upload.staging-dir:}") String configuredStagingRoot) {
        this.redisStreams = redisStreams;
        this.objectMapper = objectMapper;
        this.events = events;
        this.stagingRoot = resolveStagingRoot(configuredStagingRoot);
    }

    public String enqueue(String authenticatedEmail, Long researchGroupId, Long projectId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new InvalidProjectDatasetException("At least one file is required to upload a dataset");
        }

        String jobId = UUID.randomUUID().toString();
        List<StagedDatasetFile> stagedFiles = stageFiles(jobId, files);
        events.publish(new ProjectDatasetUploadEventDto(jobId, "QUEUED", 5, "Dataset upload queued", null),
                authenticatedEmail, projectId);

        Map<String, String> message = new LinkedHashMap<>();
        message.put("jobId", jobId);
        message.put("authenticatedEmail", authenticatedEmail);
        message.put("researchGroupId", String.valueOf(researchGroupId));
        message.put("projectId", String.valueOf(projectId));
        message.put("files", serializeFiles(stagedFiles));

        redisStreams.add(STREAM_KEY, GROUP, message);
        return jobId;
    }

    List<StagedDatasetFile> readStagedFiles(Map<Object, Object> message) {
        Object files = message.get("files");
        if (files == null) {
            throw new InvalidProjectDatasetException("Queued dataset upload files are missing");
        }

        try {
            List<StagedDatasetFile> stagedFiles = objectMapper
                    .readerForListOf(StagedDatasetFile.class)
                    .readValue(String.valueOf(files));
            stagedFiles.forEach(stagedFile -> resolveStagedPath(stagedFile.path()));
            return stagedFiles;
        } catch (IOException ex) {
            throw new InvalidProjectDatasetException("Could not read queued dataset upload files");
        }
    }

    private List<StagedDatasetFile> stageFiles(String jobId, List<MultipartFile> files) {
        try {
            Path jobDirectory = stagingRoot.resolve(jobId).normalize();
            if (!jobDirectory.startsWith(stagingRoot)) {
                throw new InvalidProjectDatasetException("Invalid dataset upload staging directory");
            }
            Files.createDirectories(jobDirectory);

            List<StagedDatasetFile> stagedFiles = new ArrayList<>();
            for (MultipartFile file : files) {
                String safeName = sanitizeFileName(file.getOriginalFilename());
                Path target = jobDirectory.resolve(UUID.randomUUID() + "-" + safeName).normalize();
                if (!target.startsWith(jobDirectory)) {
                    throw new InvalidProjectDatasetException("Invalid file name");
                }
                file.transferTo(target);
                stagedFiles.add(new StagedDatasetFile(
                        target.toString(),
                        file.getOriginalFilename(),
                        file.getContentType(),
                        file.getName()));
            }
            return stagedFiles;
        } catch (IOException ex) {
            throw new InvalidProjectDatasetException("Could not stage uploaded dataset files");
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return FALLBACK_FILE_NAME;
        }

        String normalizedName = fileName.replace('\\', '/');
        int separatorIndex = normalizedName.lastIndexOf('/');
        String baseName = separatorIndex >= 0 ? normalizedName.substring(separatorIndex + 1) : normalizedName;
        if (baseName.isBlank() || ".".equals(baseName) || "..".equals(baseName)) {
            return FALLBACK_FILE_NAME;
        }

        StringBuilder safeName = new StringBuilder(baseName.length());
        for (int index = 0; index < baseName.length(); index++) {
            char current = baseName.charAt(index);
            safeName.append(isSafeFileNameChar(current) ? current : '_');
        }

        String sanitizedName = safeName.toString();
        return sanitizedName.isBlank() ? FALLBACK_FILE_NAME : sanitizedName;
    }

    private String serializeFiles(List<StagedDatasetFile> stagedFiles) {
        try {
            return objectMapper.writeValueAsString(stagedFiles);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize dataset upload job", ex);
        }
    }

    Path resolveStagedPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new InvalidProjectDatasetException("Invalid staged dataset file path");
        }

        Path path = Path.of(rawPath).toAbsolutePath().normalize();
        if (!path.startsWith(stagingRoot)) {
            throw new InvalidProjectDatasetException("Staged dataset file is outside the staging directory");
        }
        return path;
    }

    Path resolveStagedJobDirectory(String rawPath) {
        Path path = resolveStagedPath(rawPath);
        Path jobDirectory = path.getParent();
        if (jobDirectory == null || jobDirectory.equals(stagingRoot) || !jobDirectory.startsWith(stagingRoot)) {
            throw new InvalidProjectDatasetException("Invalid staged dataset job directory");
        }
        return jobDirectory;
    }

    private Path resolveStagingRoot(String configuredStagingRoot) {
        Path root = configuredStagingRoot == null || configuredStagingRoot.isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "corpuslab-dataset-upload")
                : Path.of(configuredStagingRoot);
        return root.toAbsolutePath().normalize();
    }

    private boolean isSafeFileNameChar(char current) {
        return Character.isLetterOrDigit(current)
                || current == '.'
                || current == '-'
                || current == '_'
                || current == ' ';
    }

    record StagedDatasetFile(String path, String originalFilename, String contentType, String parameterName) {
    }
}
