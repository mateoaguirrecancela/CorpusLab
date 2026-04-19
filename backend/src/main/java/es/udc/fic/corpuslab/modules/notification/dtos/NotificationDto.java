package es.udc.fic.corpuslab.modules.notification.dtos;

import java.time.Instant;

import es.udc.fic.corpuslab.modules.notification.enums.NotificationType;

public record NotificationDto(
                Long id,
                NotificationType type,
                boolean read,
                Instant createdAt,
                String actorFullName,
                Long researchGroupId,
                String researchGroupName,
                Long invitationId,
                Long projectId,
                String projectName) {
}
