package es.udc.fic.corpuslab.modules.project.dataset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;

import es.udc.fic.corpuslab.common.redis.RedisStreamService;
import es.udc.fic.corpuslab.modules.project.dataset.dtos.DatasetItemDto;
import es.udc.fic.corpuslab.modules.project.dataset.dtos.ProjectDatasetUploadEventDto;
import es.udc.fic.corpuslab.modules.project.dataset.dtos.UploadProjectDatasetResponseDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class ProjectDatasetUploadWorkerTest {

    @Mock
    private RedisStreamService redisStreams;

    @Mock
    private ProjectDatasetItemService projectDatasetItemService;

    @Mock
    private ProjectDatasetUploadQueue uploadQueue;

    @Mock
    private ProjectDatasetUploadEvents events;

    @TempDir
    Path tempDir;

    private SimpleMeterRegistry meterRegistry;
    private ProjectDatasetUploadWorker worker;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        worker = new ProjectDatasetUploadWorker(
                redisStreams,
                projectDatasetItemService,
                uploadQueue,
                events,
                meterRegistry);
    }

    @Test
    void processNextUploadJobShouldIncreaseReadErrorCounterWhenRedisFails() {
        when(redisStreams.readPendingThenNew(ProjectDatasetUploadQueue.STREAM_KEY, ProjectDatasetUploadQueue.GROUP, "corpuslab-worker", 1))
                .thenThrow(new RuntimeException("redis unavailable"));

        worker.processNextUploadJob();

        assertThat(meterRegistry.find("corpuslab.dataset_upload.worker.read.errors").counter().count()).isEqualTo(1.0);
    }

    @Test
    void processNextUploadJobShouldAcknowledgeInvalidJobMetadata() {
        MapRecord<String, Object, Object> record = record("1-0", Map.of("jobId", "job-1"));
        when(redisStreams.readPendingThenNew(ProjectDatasetUploadQueue.STREAM_KEY, ProjectDatasetUploadQueue.GROUP, "corpuslab-worker", 1))
                .thenReturn(List.of(record));

        worker.processNextUploadJob();

        verify(redisStreams).acknowledge(ProjectDatasetUploadQueue.STREAM_KEY, ProjectDatasetUploadQueue.GROUP, record.getId());
        assertThat(jobCounter("invalid")).isEqualTo(1.0);
    }

    @Test
    void processNextUploadJobShouldPublishFailureWhenStagedFilesCannotBeParsed() {
        Map<Object, Object> payload = basePayload("job-2");
        MapRecord<String, Object, Object> record = record("2-0", payload);
        when(redisStreams.readPendingThenNew(ProjectDatasetUploadQueue.STREAM_KEY, ProjectDatasetUploadQueue.GROUP, "corpuslab-worker", 1))
                .thenReturn(List.of(record));
        doThrow(new IllegalArgumentException("bad staged files"))
                .when(uploadQueue).readStagedFiles(payload);

        worker.processNextUploadJob();

        ArgumentCaptor<ProjectDatasetUploadEventDto> eventCaptor = ArgumentCaptor.forClass(ProjectDatasetUploadEventDto.class);
        verify(events).publish(eventCaptor.capture(), eq("owner@example.com"), eq(100L));
        assertThat(eventCaptor.getValue().status()).isEqualTo("FAILED");
        verify(redisStreams).acknowledge(ProjectDatasetUploadQueue.STREAM_KEY, ProjectDatasetUploadQueue.GROUP, record.getId());
        assertThat(jobCounter("invalid")).isEqualTo(1.0);
    }

    @Test
    void processNextUploadJobShouldProcessAndCompleteValidJob() throws Exception {
        Path jobDirectory = Files.createDirectory(tempDir.resolve("job-3"));
        Path stagedFilePath = Files.writeString(jobDirectory.resolve("rows.csv"), "text");
        var stagedFile = new ProjectDatasetUploadQueue.StagedDatasetFile(
                stagedFilePath.toString(),
                "rows.csv",
                "text/csv",
                "files");
        Map<Object, Object> payload = basePayload("job-3");
        MapRecord<String, Object, Object> record = record("3-0", payload);
        UploadProjectDatasetResponseDto uploadResult = new UploadProjectDatasetResponseDto(
                100L,
                1,
                List.of(new DatasetItemDto(10L, 0, "rows.csv", "text/csv", 4L, Instant.parse("2026-05-24T10:00:00Z"))));

        when(redisStreams.readPendingThenNew(ProjectDatasetUploadQueue.STREAM_KEY, ProjectDatasetUploadQueue.GROUP, "corpuslab-worker", 1))
                .thenReturn(List.of(record));
        when(uploadQueue.readStagedFiles(payload)).thenReturn(List.of(stagedFile));
        when(uploadQueue.resolveStagedJobDirectory(stagedFilePath.toString())).thenReturn(jobDirectory);
        when(projectDatasetItemService.uploadDataset(eq("owner@example.com"), eq(11L), eq(100L), any()))
                .thenReturn(uploadResult);

        worker.processNextUploadJob();

        ArgumentCaptor<ProjectDatasetUploadEventDto> eventCaptor = ArgumentCaptor.forClass(ProjectDatasetUploadEventDto.class);
        verify(events, org.mockito.Mockito.times(2))
                .publish(eventCaptor.capture(), eq("owner@example.com"), eq(100L));
        assertThat(eventCaptor.getAllValues()).extracting(ProjectDatasetUploadEventDto::status)
                .containsExactly("RUNNING", "COMPLETED");
        verify(redisStreams).acknowledge(ProjectDatasetUploadQueue.STREAM_KEY, ProjectDatasetUploadQueue.GROUP, record.getId());
        assertThat(jobCounter("completed")).isEqualTo(1.0);
        assertThat(Files.exists(jobDirectory)).isFalse();
    }

    @Test
    void processNextUploadJobShouldPublishFailedEventWhenUploadThrows() throws Exception {
        Path jobDirectory = Files.createDirectory(tempDir.resolve("job-4"));
        Path stagedFilePath = Files.writeString(jobDirectory.resolve("rows.csv"), "text");
        var stagedFile = new ProjectDatasetUploadQueue.StagedDatasetFile(
                stagedFilePath.toString(),
                "rows.csv",
                "text/csv",
                "files");
        Map<Object, Object> payload = basePayload("job-4");
        MapRecord<String, Object, Object> record = record("4-0", payload);

        when(redisStreams.readPendingThenNew(ProjectDatasetUploadQueue.STREAM_KEY, ProjectDatasetUploadQueue.GROUP, "corpuslab-worker", 1))
                .thenReturn(List.of(record));
        when(uploadQueue.readStagedFiles(payload)).thenReturn(List.of(stagedFile));
        when(uploadQueue.resolveStagedJobDirectory(stagedFilePath.toString())).thenReturn(jobDirectory);
        when(projectDatasetItemService.uploadDataset(eq("owner@example.com"), eq(11L), eq(100L), any()))
                .thenThrow(new RuntimeException("upload failed"));

        worker.processNextUploadJob();

        ArgumentCaptor<ProjectDatasetUploadEventDto> eventCaptor = ArgumentCaptor.forClass(ProjectDatasetUploadEventDto.class);
        verify(events, org.mockito.Mockito.times(2))
                .publish(eventCaptor.capture(), eq("owner@example.com"), eq(100L));
        assertThat(eventCaptor.getAllValues()).extracting(ProjectDatasetUploadEventDto::status)
                .containsExactly("RUNNING", "FAILED");
        verify(redisStreams).acknowledge(ProjectDatasetUploadQueue.STREAM_KEY, ProjectDatasetUploadQueue.GROUP, record.getId());
        assertThat(jobCounter("failed")).isEqualTo(1.0);
        assertThat(Files.exists(jobDirectory)).isFalse();
    }

    private MapRecord<String, Object, Object> record(String id, Map<Object, Object> values) {
        return MapRecord.<String, Object, Object>create(ProjectDatasetUploadQueue.STREAM_KEY, values)
                .withId(RecordId.of(id));
    }

    private Map<Object, Object> basePayload(String jobId) {
        return Map.of(
                "jobId", jobId,
                "authenticatedEmail", "owner@example.com",
                "researchGroupId", "11",
                "projectId", "100");
    }

    private double jobCounter(String result) {
        var counter = meterRegistry.find("corpuslab.dataset_upload.worker.jobs")
                .tag("result", result)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }
}
