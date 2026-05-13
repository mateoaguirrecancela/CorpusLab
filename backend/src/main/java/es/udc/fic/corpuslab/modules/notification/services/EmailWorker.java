package es.udc.fic.corpuslab.modules.notification.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import es.udc.fic.corpuslab.common.redis.RedisStreamService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@Profile("!test")
public class EmailWorker {

    private static final Logger logger = LoggerFactory.getLogger(EmailWorker.class);
    private static final String CONSUMER_NAME = "corpuslab-email-worker";

    private final RedisStreamService redisStreams;
    private final EmailQueue emailQueue;
    private final JavaMailSender mailSender;
    private final String from;
    private final int maxAttempts;
    private final long retryBaseBackoffMs;
    private final long retryMaxBackoffMs;

    public EmailWorker(
            RedisStreamService redisStreams,
            EmailQueue emailQueue,
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@corpuslab.com}") String from,
            @Value("${app.email.retry.max-attempts:5}") int maxAttempts,
            @Value("${app.email.retry.base-backoff-ms:30000}") long retryBaseBackoffMs,
            @Value("${app.email.retry.max-backoff-ms:900000}") long retryMaxBackoffMs) {
        this.redisStreams = redisStreams;
        this.emailQueue = emailQueue;
        this.mailSender = mailSender;
        this.from = from;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryBaseBackoffMs = Math.max(1000L, retryBaseBackoffMs);
        this.retryMaxBackoffMs = Math.max(this.retryBaseBackoffMs, retryMaxBackoffMs);
    }

    @Scheduled(fixedDelayString = "${app.email.worker-delay-ms:1000}")
    public void processNextEmailJobs() {
        try {
            processBatch(redisStreams.readPending(EmailQueue.STREAM_KEY, EmailQueue.GROUP, CONSUMER_NAME, 5));
            processBatch(redisStreams.readNew(EmailQueue.STREAM_KEY, EmailQueue.GROUP, CONSUMER_NAME, 5));
        } catch (RuntimeException ex) {
            logger.error("Email worker could not read from Redis stream {}", EmailQueue.STREAM_KEY, ex);
        }
    }

    private void processRecord(MapRecord<String, Object, Object> record) {
        EmailQueue.EmailQueuedMessage queuedMessage;
        try {
            queuedMessage = emailQueue.deserialize(record.getValue());
        } catch (IllegalArgumentException ex) {
            logger.error("Discarding invalid email job {}", record.getId(), ex);
            acknowledge(record);
            return;
        }

        if (queuedMessage.availableAtEpochMs() > System.currentTimeMillis()) {
            return;
        }

        try {
            sendHtmlEmail(queuedMessage.job());
            acknowledge(record);
        } catch (MessagingException | MailException ex) {
            handleDeliveryFailure(record, queuedMessage, ex);
        }
    }

    private void processBatch(List<MapRecord<String, Object, Object>> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (MapRecord<String, Object, Object> record : records) {
            processRecord(record);
        }
    }

    private void acknowledge(MapRecord<String, Object, Object> record) {
        redisStreams.acknowledge(EmailQueue.STREAM_KEY, EmailQueue.GROUP, record.getId());
    }

    private void sendHtmlEmail(EmailJob job) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(from);
        helper.setTo(job.to());
        helper.setSubject(job.subject());
        helper.setText(job.content(), true);
        mailSender.send(message);
    }

    private void handleDeliveryFailure(
            MapRecord<String, Object, Object> record,
            EmailQueue.EmailQueuedMessage queuedMessage,
            Exception ex) {
        int deliveryAttempt = queuedMessage.attempts() + 1;

        if (deliveryAttempt >= maxAttempts) {
            logger.error(
                    "Email delivery exhausted retries for {} after {} attempts. Moving to dead-letter stream",
                    queuedMessage.job().to(),
                    deliveryAttempt,
                    ex);
            emailQueue.deadLetter(queuedMessage, ex.getMessage());
            acknowledge(record);
            return;
        }

        long nextAvailableAt = System.currentTimeMillis() + computeBackoffMs(deliveryAttempt);
        logger.warn(
                "Email delivery failed for {} on attempt {} of {}. Scheduling retry",
                queuedMessage.job().to(),
                deliveryAttempt,
                maxAttempts,
                ex);
        emailQueue.enqueue(queuedMessage.job(), deliveryAttempt, nextAvailableAt);
        acknowledge(record);
    }

    private long computeBackoffMs(int deliveryAttempt) {
        long exponentialBackoff = retryBaseBackoffMs;
        for (int currentAttempt = 1; currentAttempt < deliveryAttempt; currentAttempt++) {
            if (exponentialBackoff >= retryMaxBackoffMs) {
                return retryMaxBackoffMs;
            }
            exponentialBackoff = Math.min(retryMaxBackoffMs, exponentialBackoff * 2L);
        }
        return exponentialBackoff;
    }

}
