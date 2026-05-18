package es.udc.fic.corpuslab.modules.project.dataset;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import es.udc.fic.corpuslab.common.redis.RedisStreamService;
import es.udc.fic.corpuslab.modules.project.dataset.dtos.ProjectDatasetUploadEventDto;
import es.udc.fic.corpuslab.modules.project.dataset.dtos.UploadProjectDatasetResponseDto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class ProjectDatasetUploadWorker {

    private static final Logger logger = LoggerFactory.getLogger(ProjectDatasetUploadWorker.class);
    private static final String CONSUMER_NAME = "corpuslab-worker";
    private static final String STATE_IDLE = "idle";
    private static final String STATE_POLLING = "polling";
    private static final String STATE_PROCESSING = "processing";

    private final RedisStreamService redisStreams;
    private final ProjectDatasetItemService projectDatasetItemService;
    private final ProjectDatasetUploadQueue uploadQueue;
    private final ProjectDatasetUploadEvents events;
    private final MeterRegistry meterRegistry;
    private final Counter completedJobs;
    private final Counter failedJobs;
    private final Counter invalidJobs;
    private final Counter readErrors;
    private final Timer jobDuration;
    private final AtomicInteger activeJobs = new AtomicInteger();
    private final AtomicInteger idleState = new AtomicInteger(1);
    private final AtomicInteger pollingState = new AtomicInteger();
    private final AtomicInteger processingState = new AtomicInteger();
    private final AtomicLong lastPollEpochSeconds = new AtomicLong();
    private final AtomicLong lastSuccessEpochSeconds = new AtomicLong();
    private final AtomicLong lastFailureEpochSeconds = new AtomicLong();

    public ProjectDatasetUploadWorker(
            RedisStreamService redisStreams,
            ProjectDatasetItemService projectDatasetItemService,
            ProjectDatasetUploadQueue uploadQueue,
            ProjectDatasetUploadEvents events,
            MeterRegistry meterRegistry) {
        this.redisStreams = redisStreams;
        this.projectDatasetItemService = projectDatasetItemService;
        this.uploadQueue = uploadQueue;
        this.events = events;
        this.meterRegistry = meterRegistry;
        this.completedJobs = jobCounter(meterRegistry, "completed");
        this.failedJobs = jobCounter(meterRegistry, "failed");
        this.invalidJobs = jobCounter(meterRegistry, "invalid");
        this.readErrors = Counter.builder("corpuslab.dataset_upload.worker.read.errors")
                .description("Redis Stream read errors observed by the dataset upload worker")
                .tag("worker", CONSUMER_NAME)
                .tag("stream", ProjectDatasetUploadQueue.STREAM_KEY)
                .register(meterRegistry);
        this.jobDuration = Timer.builder("corpuslab.dataset_upload.worker.job.duration")
                .description("Time spent processing dataset upload jobs")
                .tag("worker", CONSUMER_NAME)
                .tag("stream", ProjectDatasetUploadQueue.STREAM_KEY)
                .register(meterRegistry);
        registerWorkerGauges(meterRegistry);
        registerQueueGauges(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${app.dataset-upload.worker-delay-ms:1000}")
    public void processNextUploadJob() {
        lastPollEpochSeconds.set(currentEpochSeconds());
        setWorkerState(STATE_POLLING);

        List<MapRecord<String, Object, Object>> records;
        try {
            records = readPendingThenNew();
        } catch (RuntimeException ex) {
            readErrors.increment();
            logger.error("Dataset upload worker could not read from Redis stream {}", ProjectDatasetUploadQueue.STREAM_KEY, ex);
            setWorkerState(STATE_IDLE);
            return;
        }

        if (records == null || records.isEmpty()) {
            setWorkerState(STATE_IDLE);
            return;
        }

        try {
            for (MapRecord<String, Object, Object> record : records) {
                processRecord(record);
            }
        } finally {
            setWorkerState(STATE_IDLE);
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
        activeJobs.incrementAndGet();
        setWorkerState(STATE_PROCESSING);
        try {
            processRecordInternal(record);
        } finally {
            activeJobs.decrementAndGet();
        }
    }

    private void processRecordInternal(MapRecord<String, Object, Object> record) {
        Map<Object, Object> rawMessage = record.getValue();
        DatasetUploadJobMetadata metadata;
        try {
            metadata = parseJobMetadata(rawMessage);
        } catch (RuntimeException ex) {
            logger.error("Discarding invalid dataset upload job {}", record.getId(), ex);
            acknowledge(record);
            invalidJobs.increment();
            lastFailureEpochSeconds.set(currentEpochSeconds());
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
            invalidJobs.increment();
            lastFailureEpochSeconds.set(currentEpochSeconds());
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
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
            completedJobs.increment();
            lastSuccessEpochSeconds.set(currentEpochSeconds());
        } catch (RuntimeException ex) {
            logger.error("Dataset upload job {} failed", jobMessage.jobId(), ex);
            events.publish(new ProjectDatasetUploadEventDto(
                    jobMessage.jobId(), "FAILED", 100, ex.getMessage(), null),
                    jobMessage.authenticatedEmail(), jobMessage.projectId());
            acknowledge(record);
            failedJobs.increment();
            lastFailureEpochSeconds.set(currentEpochSeconds());
        } finally {
            sample.stop(jobDuration);
            cleanup(jobMessage.stagedFiles());
        }
    }

    private Counter jobCounter(MeterRegistry meterRegistry, String result) {
        return Counter.builder("corpuslab.dataset_upload.worker.jobs")
                .description("Dataset upload jobs processed by the worker")
                .tag("worker", CONSUMER_NAME)
                .tag("stream", ProjectDatasetUploadQueue.STREAM_KEY)
                .tag("result", result)
                .register(meterRegistry);
    }

    private void registerWorkerGauges(MeterRegistry meterRegistry) {
        workerGauge("corpuslab.dataset_upload.worker.active.jobs", "Dataset upload jobs currently being processed",
                activeJobs);
        workerGauge("corpuslab.dataset_upload.worker.state", "Current dataset upload worker state", idleState,
                "state", STATE_IDLE);
        workerGauge("corpuslab.dataset_upload.worker.state", "Current dataset upload worker state", pollingState,
                "state", STATE_POLLING);
        workerGauge("corpuslab.dataset_upload.worker.state", "Current dataset upload worker state", processingState,
                "state", STATE_PROCESSING);
        workerGauge("corpuslab.dataset_upload.worker.last.poll.timestamp",
                "Unix timestamp of the last dataset upload worker poll", lastPollEpochSeconds);
        workerGauge("corpuslab.dataset_upload.worker.last.success.timestamp",
                "Unix timestamp of the last successful dataset upload job", lastSuccessEpochSeconds);
        workerGauge("corpuslab.dataset_upload.worker.last.failure.timestamp",
                "Unix timestamp of the last failed or invalid dataset upload job", lastFailureEpochSeconds);
    }

    private void registerQueueGauges(MeterRegistry meterRegistry) {
        queueGauge("corpuslab.dataset_upload.queue.stream.size",
                "Redis Stream length for dataset upload jobs",
                () -> redisStreams.size(ProjectDatasetUploadQueue.STREAM_KEY));
        queueGauge("corpuslab.dataset_upload.queue.pending.messages",
                "Pending dataset upload messages in the Redis Stream consumer group",
                () -> redisStreams.pending(ProjectDatasetUploadQueue.STREAM_KEY, ProjectDatasetUploadQueue.GROUP));
        queueGauge("corpuslab.dataset_upload.queue.lag.messages",
                "Undelivered dataset upload messages behind the Redis Stream consumer group",
                () -> redisStreams.lag(ProjectDatasetUploadQueue.STREAM_KEY, ProjectDatasetUploadQueue.GROUP));
        queueGauge("corpuslab.dataset_upload.queue.consumers",
                "Registered consumers in the dataset upload Redis Stream consumer group",
                () -> redisStreams.consumerCount(ProjectDatasetUploadQueue.STREAM_KEY, ProjectDatasetUploadQueue.GROUP));
        queueGauge("corpuslab.dataset_upload.queue.consumer.pending.messages",
                "Pending dataset upload messages assigned to this worker",
                () -> redisStreams.pending(
                        ProjectDatasetUploadQueue.STREAM_KEY,
                        ProjectDatasetUploadQueue.GROUP,
                        CONSUMER_NAME),
                "worker", CONSUMER_NAME);
        queueGauge("corpuslab.dataset_upload.queue.consumer.idle.seconds",
                "Seconds since Redis last saw activity for this dataset upload worker consumer",
                () -> redisStreams.consumerIdleTimeSeconds(
                        ProjectDatasetUploadQueue.STREAM_KEY,
                        ProjectDatasetUploadQueue.GROUP,
                        CONSUMER_NAME),
                "worker", CONSUMER_NAME);
    }

    private void workerGauge(String name, String description, AtomicInteger value, String... extraTags) {
        Gauge.Builder<AtomicInteger> builder = Gauge.builder(name, value, AtomicInteger::get)
                .description(description)
                .tag("worker", CONSUMER_NAME)
                .tag("stream", ProjectDatasetUploadQueue.STREAM_KEY);
        for (int index = 0; index < extraTags.length; index += 2) {
            builder.tag(extraTags[index], extraTags[index + 1]);
        }
        builder.register(meterRegistry);
    }

    private void workerGauge(String name, String description, AtomicLong value) {
        Gauge.builder(name, value, AtomicLong::get)
                .description(description)
                .tag("worker", CONSUMER_NAME)
                .tag("stream", ProjectDatasetUploadQueue.STREAM_KEY)
                .register(meterRegistry);
    }

    private void queueGauge(String name, String description, LongSupplier supplier, String... extraTags) {
        Gauge.Builder<ProjectDatasetUploadWorker> builder = Gauge.builder(
                name,
                this,
                ignored -> safeGaugeValue(supplier))
                .description(description)
                .tag("stream", ProjectDatasetUploadQueue.STREAM_KEY)
                .tag("group", ProjectDatasetUploadQueue.GROUP);
        for (int index = 0; index < extraTags.length; index += 2) {
            builder.tag(extraTags[index], extraTags[index + 1]);
        }
        builder.register(meterRegistry);
    }

    private double safeGaugeValue(LongSupplier supplier) {
        try {
            return supplier.getAsLong();
        } catch (RuntimeException ex) {
            return Double.NaN;
        }
    }

    private void setWorkerState(String state) {
        idleState.set(STATE_IDLE.equals(state) ? 1 : 0);
        pollingState.set(STATE_POLLING.equals(state) ? 1 : 0);
        processingState.set(STATE_PROCESSING.equals(state) ? 1 : 0);
    }

    private long currentEpochSeconds() {
        return System.currentTimeMillis() / 1000L;
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
