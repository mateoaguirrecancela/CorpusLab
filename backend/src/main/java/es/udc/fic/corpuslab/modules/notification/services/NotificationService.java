package es.udc.fic.corpuslab.modules.notification.services;

import es.udc.fic.corpuslab.modules.notification.dtos.NotificationDto;
import es.udc.fic.corpuslab.modules.notification.dtos.NotificationListResponseDto;

/**
 * Public API for the notification module.
 * All method signatures use exclusively primitives and DTOs — no entity imports.
 */
public interface NotificationService {

    NotificationListResponseDto findMyNotifications(String authenticatedEmail, int limit);

    NotificationDto markNotificationAsRead(String authenticatedEmail, Long notificationId);

    void markAllNotificationsAsRead(String authenticatedEmail);

    void createResearchGroupInvitationReceivedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long researchGroupId,
            String researchGroupName,
            Long invitationId);

    void createResearchGroupInvitationAcceptedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long researchGroupId,
            String researchGroupName);

    void createProjectParticipantAssignedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long projectId,
            String projectName,
            Long researchGroupId,
            String researchGroupName);

    void createProjectAnnotationCompletedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long projectId,
            String projectName,
            Long researchGroupId,
            String researchGroupName);

    void deleteNotificationsByProjectId(Long projectId);

    void deleteNotificationsByResearchGroupId(Long researchGroupId);
}
