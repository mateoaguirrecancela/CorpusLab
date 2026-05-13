package es.udc.fic.corpuslab.modules.project.services;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.udc.fic.corpuslab.modules.project.dtos.ProjectDatasetUploadEventDto;
import es.udc.fic.corpuslab.modules.project.dtos.UploadProjectDatasetResponseDto;

@Service
public class ProjectDatasetUploadStatusStore {

    private static final String KEY_PREFIX = "corpuslab:dataset-upload:job:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration statusTtl;

    public ProjectDatasetUploadStatusStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.dataset-upload.status-ttl-seconds:86400}") long statusTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.statusTtl = Duration.ofSeconds(Math.max(60L, statusTtlSeconds));
    }

    public void save(ProjectDatasetUploadEventDto event, String ownerEmail, Long projectId) {
        String key = key(event.jobId());
        redisTemplate.opsForHash().putAll(key, Map.of(
                "jobId", event.jobId(),
                "status", event.status(),
                "progress", String.valueOf(event.progress()),
                "message", event.message() == null ? "" : event.message(),
                "ownerEmail", ownerEmail,
                "projectId", String.valueOf(projectId),
                "result", serializeResult(event.result())));
        redisTemplate.expire(key, statusTtl);
    }

    public Optional<ProjectDatasetUploadEventDto> findEvent(String jobId) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(key(jobId));
        if (values.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ProjectDatasetUploadEventDto(
                stringValue(values.get("jobId")),
                stringValue(values.get("status")),
                intValue(values.get("progress")),
                blankToNull(stringValue(values.get("message"))),
                deserializeResult(stringValue(values.get("result")))));
    }

    public Optional<String> findOwnerEmail(String jobId) {
        Object ownerEmail = redisTemplate.opsForHash().get(key(jobId), "ownerEmail");
        return ownerEmail == null ? Optional.empty() : Optional.of(String.valueOf(ownerEmail));
    }

    public Optional<Long> findProjectId(String jobId) {
        Object projectId = redisTemplate.opsForHash().get(key(jobId), "projectId");
        if (projectId == null) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(String.valueOf(projectId)));
    }

    private String serializeResult(UploadProjectDatasetResponseDto result) {
        if (result == null) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize dataset upload result", ex);
        }
    }

    private UploadProjectDatasetResponseDto deserializeResult(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(resultJson, UploadProjectDatasetResponseDto.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not deserialize dataset upload result", ex);
        }
    }

    private String key(String jobId) {
        return KEY_PREFIX + jobId;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private int intValue(Object value) {
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
