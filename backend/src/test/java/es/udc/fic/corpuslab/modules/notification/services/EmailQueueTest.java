package es.udc.fic.corpuslab.modules.notification.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.udc.fic.corpuslab.common.redis.RedisStreamService;

@ExtendWith(MockitoExtension.class)
class EmailQueueTest {

    @Mock
    private RedisStreamService redisStreams;

    private EmailQueue emailQueue;

    @BeforeEach
    void setUp() {
        emailQueue = new EmailQueue(redisStreams, new ObjectMapper());
    }

    @Test
    void enqueueShouldSerializePayloadWithDefaultAttemptMetadata() throws Exception {
        EmailJob job = new EmailJob("to@example.com", "Subject", "<p>Body</p>");

        emailQueue.enqueue(job);

        ArgumentCaptor<Map<String, String>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(redisStreams).add(eq("corpuslab:emails"), eq("email-workers"), messageCaptor.capture());

        Map<String, String> payload = messageCaptor.getValue();
        assertThat(payload.get("attempts")).isEqualTo("0");
        assertThat(payload.get("availableAtEpochMs")).isNotBlank();
        assertThat(payload.get("payload")).contains("\"to\":\"to@example.com\"");
    }

    @Test
    void enqueueShouldKeepProvidedAttemptsAndAvailableAt() {
        EmailJob job = new EmailJob("to@example.com", "Subject", "<p>Body</p>");

        emailQueue.enqueue(job, 2, 123456789L);

        ArgumentCaptor<Map<String, String>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(redisStreams).add(eq("corpuslab:emails"), eq("email-workers"), messageCaptor.capture());
        assertThat(messageCaptor.getValue())
                .containsEntry("attempts", "2")
                .containsEntry("availableAtEpochMs", "123456789");
    }

    @Test
    void deadLetterShouldAppendSerializedFailureMetadata() {
        EmailQueue.EmailQueuedMessage queuedMessage = new EmailQueue.EmailQueuedMessage(
                new EmailJob("to@example.com", "Subject", "<p>Body</p>"),
                3,
                111L);
        when(redisStreams.size("corpuslab:emails:dead-letter")).thenReturn(4L);

        emailQueue.deadLetter(queuedMessage, "smtp down");

        ArgumentCaptor<Map<String, String>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(redisStreams).append(eq("corpuslab:emails:dead-letter"), messageCaptor.capture());
        assertThat(messageCaptor.getValue())
                .containsEntry("attempts", "3")
                .containsEntry("error", "smtp down");
        assertThat(messageCaptor.getValue().get("payload")).contains("\"subject\":\"Subject\"");
    }

    @Test
    void deserializeShouldParseValidQueuedMessage() {
        Map<Object, Object> message = Map.of(
                "payload", "{\"to\":\"to@example.com\",\"subject\":\"Subject\",\"content\":\"Body\"}",
                "attempts", "4",
                "availableAtEpochMs", "1234");

        EmailQueue.EmailQueuedMessage queuedMessage = emailQueue.deserialize(message);

        assertThat(queuedMessage.job().to()).isEqualTo("to@example.com");
        assertThat(queuedMessage.attempts()).isEqualTo(4);
        assertThat(queuedMessage.availableAtEpochMs()).isEqualTo(1234L);
    }

    @Test
    void deserializeShouldRejectMessageWithoutPayload() {
        assertThatThrownBy(() -> emailQueue.deserialize(Map.of("attempts", "1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payload is missing");
    }

    @Test
    void deserializeShouldRejectMessageWithInvalidAttempts() {
        Map<Object, Object> message = Map.of(
                "payload", "{\"to\":\"to@example.com\",\"subject\":\"Subject\",\"content\":\"Body\"}",
                "attempts", "oops",
                "availableAtEpochMs", "1234");

        assertThatThrownBy(() -> emailQueue.deserialize(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attempts is invalid");
    }
}
