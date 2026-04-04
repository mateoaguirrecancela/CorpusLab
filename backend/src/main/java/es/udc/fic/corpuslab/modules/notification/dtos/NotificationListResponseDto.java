package es.udc.fic.corpuslab.modules.notification.dtos;

import java.util.List;

public record NotificationListResponseDto(
        List<NotificationDto> notifications,
        long unreadCount) {
}
