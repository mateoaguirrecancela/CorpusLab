package es.udc.fic.corpuslab.modules.notification.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import es.udc.fic.corpuslab.common.redis.RedisStreamService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailWorkerTest {

    @Mock
    private RedisStreamService redisStreams;

    @Mock
    private EmailQueue emailQueue;

    @Mock
    private JavaMailSender mailSender;

    private EmailWorker emailWorker;

    @BeforeEach
    void setUp() {
        emailWorker = new EmailWorker(
                redisStreams,
                emailQueue,
                mailSender,
                "no-reply@example.com",
                3,
                1000L,
                4000L);
    }

    @Test
    void processNextEmailJobsShouldIgnoreRedisReadErrors() {
        when(redisStreams.readNewThenPending("corpuslab:emails", "email-workers", "corpuslab-email-worker", 5))
                .thenThrow(new RuntimeException("redis unavailable"));

        emailWorker.processNextEmailJobs();

        verify(redisStreams).readNewThenPending("corpuslab:emails", "email-workers", "corpuslab-email-worker", 5);
    }

    @Test
    void processNextEmailJobsShouldAcknowledgeInvalidQueuedMessages() {
        MapRecord<String, Object, Object> record = record("1-0", Map.of());
        when(redisStreams.readNewThenPending("corpuslab:emails", "email-workers", "corpuslab-email-worker", 5))
                .thenReturn(List.of(record));
        doThrow(new IllegalArgumentException("bad payload"))
                .when(emailQueue).deserialize(record.getValue());

        emailWorker.processNextEmailJobs();

        verify(redisStreams).acknowledge("corpuslab:emails", "email-workers", record.getId());
    }

    @Test
    void processNextEmailJobsShouldSkipMessagesScheduledForFuture() {
        MapRecord<String, Object, Object> record = record("2-0", Map.of());
        EmailQueue.EmailQueuedMessage queuedMessage = new EmailQueue.EmailQueuedMessage(
                new EmailJob("user@example.com", "Subject", "Body"),
                0,
                System.currentTimeMillis() + 60_000L);
        when(redisStreams.readNewThenPending("corpuslab:emails", "email-workers", "corpuslab-email-worker", 5))
                .thenReturn(List.of(record));
        when(emailQueue.deserialize(record.getValue())).thenReturn(queuedMessage);

        emailWorker.processNextEmailJobs();

        verify(redisStreams, never()).acknowledge("corpuslab:emails", "email-workers", record.getId());
    }

    @Test
    void processNextEmailJobsShouldAcknowledgeAfterSuccessfulDelivery() {
        MapRecord<String, Object, Object> record = record("3-0", Map.of());
        EmailQueue.EmailQueuedMessage queuedMessage = new EmailQueue.EmailQueuedMessage(
                new EmailJob("user@example.com", "Subject", "<p>Body</p>"),
                0,
                System.currentTimeMillis() - 1L);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(redisStreams.readNewThenPending("corpuslab:emails", "email-workers", "corpuslab-email-worker", 5))
                .thenReturn(List.of(record));
        when(emailQueue.deserialize(record.getValue())).thenReturn(queuedMessage);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailWorker.processNextEmailJobs();

        verify(mailSender).send(mimeMessage);
        verify(redisStreams).acknowledge("corpuslab:emails", "email-workers", record.getId());
        verify(emailQueue, never()).enqueue(any(EmailJob.class), any(Integer.class), any(Long.class));
        verify(emailQueue, never()).deadLetter(any(), any());
    }

    @Test
    void processNextEmailJobsShouldScheduleRetryOnDeliveryFailureBeforeMaxAttempts() {
        MapRecord<String, Object, Object> record = record("4-0", Map.of());
        EmailJob job = new EmailJob("user@example.com", "Subject", "<p>Body</p>");
        EmailQueue.EmailQueuedMessage queuedMessage = new EmailQueue.EmailQueuedMessage(
                job,
                1,
                System.currentTimeMillis() - 1L);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(redisStreams.readNewThenPending("corpuslab:emails", "email-workers", "corpuslab-email-worker", 5))
                .thenReturn(List.of(record));
        when(emailQueue.deserialize(record.getValue())).thenReturn(queuedMessage);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(mimeMessage);

        long before = System.currentTimeMillis();
        emailWorker.processNextEmailJobs();
        long after = System.currentTimeMillis();

        ArgumentCaptor<Long> availableAtCaptor = ArgumentCaptor.forClass(Long.class);
        verify(emailQueue).enqueue(eq(job), eq(2), availableAtCaptor.capture());
        verify(redisStreams).acknowledge("corpuslab:emails", "email-workers", record.getId());
        assertThat(availableAtCaptor.getValue()).isBetween(before + 1000L, after + 4000L);
        verify(emailQueue, never()).deadLetter(any(), any());
    }

    @Test
    void processNextEmailJobsShouldSendToDeadLetterWhenAttemptsAreExhausted() {
        MapRecord<String, Object, Object> record = record("5-0", Map.of());
        EmailJob job = new EmailJob("user@example.com", "Subject", "<p>Body</p>");
        EmailQueue.EmailQueuedMessage queuedMessage = new EmailQueue.EmailQueuedMessage(
                job,
                2,
                System.currentTimeMillis() - 1L);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(redisStreams.readNewThenPending("corpuslab:emails", "email-workers", "corpuslab-email-worker", 5))
                .thenReturn(List.of(record));
        when(emailQueue.deserialize(record.getValue())).thenReturn(queuedMessage);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(mimeMessage);

        emailWorker.processNextEmailJobs();

        verify(emailQueue).deadLetter(eq(queuedMessage), eq("smtp down"));
        verify(redisStreams).acknowledge("corpuslab:emails", "email-workers", record.getId());
        verify(emailQueue, never()).enqueue(any(EmailJob.class), any(Integer.class), any(Long.class));
    }

    private MapRecord<String, Object, Object> record(String id, Map<Object, Object> payload) {
        return MapRecord.<String, Object, Object>create("corpuslab:emails", payload)
                .withId(RecordId.of(id));
    }
}
