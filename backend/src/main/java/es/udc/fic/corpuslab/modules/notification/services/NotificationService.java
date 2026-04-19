package es.udc.fic.corpuslab.modules.notification.services;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.notification.dtos.NotificationDto;
import es.udc.fic.corpuslab.modules.notification.dtos.NotificationListResponseDto;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;

public interface NotificationService {

        NotificationListResponseDto findMyNotifications(String authenticatedEmail, int limit);

        NotificationDto markNotificationAsRead(String authenticatedEmail, Long notificationId);

        void markAllNotificationsAsRead(String authenticatedEmail);

        void createResearchGroupInvitationReceivedNotification(
                        User recipient,
                        User inviter,
                        ResearchGroup researchGroup,
                        Long invitationId);

        void createResearchGroupInvitationAcceptedNotification(
                        User recipient,
                        User actor,
                        ResearchGroup researchGroup);

        void createProjectParticipantAssignedNotification(
                        User recipient,
                        User actor,
                        Project project);

        void createProjectAnnotationCompletedNotification(
                        User recipient,
                        User actor,
                        Project project);
}
