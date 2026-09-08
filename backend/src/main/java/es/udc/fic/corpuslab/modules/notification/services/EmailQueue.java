package es.udc.fic.corpuslab.modules.notification.services;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.udc.fic.corpuslab.common.redis.RedisStreamService;

@Service
@Profile("!test")
public class EmailQueue {

    private static final Logger logger = LoggerFactory.getLogger(EmailQueue.class);

    static final String STREAM_KEY = "corpuslab:emails";
    static final String GROUP = "email-workers";
    static final String DEAD_LETTER_STREAM_KEY = "corpuslab:emails:dead-letter";
    private static final String FIELD_PAYLOAD = "payload";
    private static final String FIELD_ATTEMPTS = "attempts";
    private static final String FIELD_AVAILABLE_AT_EPOCH_MS = "availableAtEpochMs";
    private static final String FIELD_FAILED_AT_EPOCH_MS = "failedAtEpochMs";
    private static final String FIELD_ERROR = "error";

    private final RedisStreamService redisStreams;
    private final ObjectMapper objectMapper;

    public EmailQueue(RedisStreamService redisStreams, ObjectMapper objectMapper) {
        this.redisStreams = redisStreams;
        this.objectMapper = objectMapper;
    }

    public void enqueue(EmailJob job) {
        enqueue(job, 0, System.currentTimeMillis());
    }

    public void enqueue(EmailJob job, int attempts, long availableAtEpochMs) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put(FIELD_PAYLOAD, serialize(job));
        message.put(FIELD_ATTEMPTS, String.valueOf(attempts));
        message.put(FIELD_AVAILABLE_AT_EPOCH_MS, String.valueOf(availableAtEpochMs));
        redisStreams.add(STREAM_KEY, GROUP, message);
    }

    public void deadLetter(EmailQueuedMessage message, String error) {
        Map<String, String> deadLetterMessage = new LinkedHashMap<>();
        deadLetterMessage.put(FIELD_PAYLOAD, serialize(message.job()));
        deadLetterMessage.put(FIELD_ATTEMPTS, String.valueOf(message.attempts()));
        deadLetterMessage.put(FIELD_FAILED_AT_EPOCH_MS, String.valueOf(System.currentTimeMillis()));
        deadLetterMessage.put(FIELD_ERROR, error == null ? "" : error);
        redisStreams.append(DEAD_LETTER_STREAM_KEY, deadLetterMessage);
        logger.error(
                "Email job moved to dead-letter stream {}. recipient={}, attempts={}, deadLetterSize={}",
                DEAD_LETTER_STREAM_KEY,
                message.job().to(),
                message.attempts(),
                redisStreams.size(DEAD_LETTER_STREAM_KEY));
    }

    EmailQueuedMessage deserialize(Map<Object, Object> message) {
        Object payload = message.get(FIELD_PAYLOAD);
        if (payload == null) {
            throw new IllegalArgumentException("Email job payload is missing");
        }
        try {
            EmailJob job = objectMapper.readValue(String.valueOf(payload), EmailJob.class);
            int attempts = parseInteger(message.get(FIELD_ATTEMPTS), FIELD_ATTEMPTS);
            long availableAtEpochMs = parseLong(message.get(FIELD_AVAILABLE_AT_EPOCH_MS), FIELD_AVAILABLE_AT_EPOCH_MS);
            return new EmailQueuedMessage(job, attempts, availableAtEpochMs);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Email job payload is invalid", ex);
        }
    }

    private String serialize(EmailJob job) {
        try {
            return objectMapper.writeValueAsString(job);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize email job", ex);
        }
    }

    private int parseInteger(Object value, String fieldName) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Email job " + fieldName + " is invalid", ex);
        }
    }

    private long parseLong(Object value, String fieldName) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Email job " + fieldName + " is invalid", ex);
        }
    }

    record EmailQueuedMessage(EmailJob job, int attempts, long availableAtEpochMs) {
    }

}
