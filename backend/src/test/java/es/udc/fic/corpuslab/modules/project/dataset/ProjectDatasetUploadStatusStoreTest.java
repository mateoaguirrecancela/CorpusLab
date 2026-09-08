package es.udc.fic.corpuslab.modules.project.dataset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.udc.fic.corpuslab.modules.project.dataset.dtos.DatasetItemDto;
import es.udc.fic.corpuslab.modules.project.dataset.dtos.ProjectDatasetUploadEventDto;
import es.udc.fic.corpuslab.modules.project.dataset.dtos.UploadProjectDatasetResponseDto;

@ExtendWith(MockitoExtension.class)
class ProjectDatasetUploadStatusStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private ObjectMapper objectMapper;

    private ProjectDatasetUploadStatusStore statusStore;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        statusStore = new ProjectDatasetUploadStatusStore(redisTemplate, objectMapper, 10L);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void saveShouldPersistStatusPayloadAndApplyMinimumTtl() {
        UploadProjectDatasetResponseDto result = new UploadProjectDatasetResponseDto(
                15L,
                1,
                List.of(new DatasetItemDto(1L, 0, "rows.csv", "text/csv", 10L, Instant.parse("2026-05-24T10:00:00Z"))));
        ProjectDatasetUploadEventDto event = new ProjectDatasetUploadEventDto(
                "job-123",
                "COMPLETED",
                100,
                "done",
                result);

        statusStore.save(event, "owner@example.com", 15L);

        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(eq("corpuslab:dataset-upload:job:job-123"), payloadCaptor.capture());
        verify(redisTemplate).expire("corpuslab:dataset-upload:job:job-123", Duration.ofSeconds(60));

        assertThat(payloadCaptor.getValue())
                .containsEntry("jobId", "job-123")
                .containsEntry("status", "COMPLETED")
                .containsEntry("progress", "100")
                .containsEntry("message", "done")
                .containsEntry("ownerEmail", "owner@example.com")
                .containsEntry("projectId", "15");
        assertThat(payloadCaptor.getValue().get("result")).contains("\"projectId\":15");
    }

    @Test
    void findEventShouldDeserializeStoredPayload() throws Exception {
        UploadProjectDatasetResponseDto result = new UploadProjectDatasetResponseDto(20L, 2, List.of());
        String resultJson = objectMapper.writeValueAsString(result);
        when(hashOperations.entries("corpuslab:dataset-upload:job:job-1"))
                .thenReturn(Map.of(
                        "jobId", "job-1",
                        "status", "RUNNING",
                        "progress", "40",
                        "message", "",
                        "result", resultJson));

        var event = statusStore.findEvent("job-1").orElseThrow();

        assertThat(event.jobId()).isEqualTo("job-1");
        assertThat(event.status()).isEqualTo("RUNNING");
        assertThat(event.progress()).isEqualTo(40);
        assertThat(event.message()).isNull();
        assertThat(event.result()).isEqualTo(result);
    }

    @Test
    void findEventShouldReturnEmptyWhenJobIsUnknown() {
        when(hashOperations.entries("corpuslab:dataset-upload:job:missing")).thenReturn(Map.of());

        assertThat(statusStore.findEvent("missing")).isEmpty();
    }

    @Test
    void findOwnerEmailAndProjectIdShouldReadStoredMetadata() {
        when(hashOperations.get("corpuslab:dataset-upload:job:job-7", "ownerEmail"))
                .thenReturn("owner@example.com");
        when(hashOperations.get("corpuslab:dataset-upload:job:job-7", "projectId"))
                .thenReturn("99");

        assertThat(statusStore.findOwnerEmail("job-7")).contains("owner@example.com");
        assertThat(statusStore.findProjectId("job-7")).contains(99L);
    }

    @Test
    void findEventShouldFallbackToZeroProgressWhenStoredProgressIsInvalid() {
        when(hashOperations.entries("corpuslab:dataset-upload:job:job-2"))
                .thenReturn(Map.of(
                        "jobId", "job-2",
                        "status", "QUEUED",
                        "progress", "not-a-number",
                        "message", "queued",
                        "result", ""));

        var event = statusStore.findEvent("job-2").orElseThrow();

        assertThat(event.progress()).isZero();
    }
}
