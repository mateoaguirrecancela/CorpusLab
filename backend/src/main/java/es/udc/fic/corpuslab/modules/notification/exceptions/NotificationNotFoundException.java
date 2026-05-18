package es.udc.fic.corpuslab.modules.notification.exceptions;

import org.springframework.http.HttpStatus;

import es.udc.fic.corpuslab.common.exceptions.TranslatableApiException;

public class NotificationNotFoundException extends RuntimeException implements TranslatableApiException {

    private final Long notificationId;

    public NotificationNotFoundException(Long notificationId) {
        super("Notification not found: " + notificationId);
        this.notificationId = notificationId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    @Override
    public String getMessageKey() {
        return "notification.error.notfound";
    }

    @Override
    public Object[] getMessageArgs() {
        return new Object[] { notificationId };
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
