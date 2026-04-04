package es.udc.fic.corpuslab.modules.notification.exceptions;

public class NotificationNotFoundException extends RuntimeException {

    private final Long notificationId;

    public NotificationNotFoundException(Long notificationId) {
        super("Notification not found: " + notificationId);
        this.notificationId = notificationId;
    }

    public Long getNotificationId() {
        return notificationId;
    }
}
