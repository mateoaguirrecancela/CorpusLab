package es.udc.fic.corpuslab.modules.notification.services;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.notification.dtos.NotificationDto;
import es.udc.fic.corpuslab.modules.notification.dtos.NotificationListResponseDto;
import es.udc.fic.corpuslab.modules.notification.entities.Notification;
import es.udc.fic.corpuslab.modules.notification.enums.NotificationType;
import es.udc.fic.corpuslab.modules.notification.exceptions.NotificationNotFoundException;
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import jakarta.persistence.EntityManager;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final AuthApiService authApiService;
    private final NotificationRepository notificationRepository;
    private final EntityManager entityManager;
    private final NotificationEvents notificationEvents;

    @Autowired
    public NotificationServiceImpl(
            AuthApiService authApiService,
            NotificationRepository notificationRepository,
            EntityManager entityManager,
            NotificationEvents notificationEvents) {
        this.authApiService = authApiService;
        this.notificationRepository = notificationRepository;
        this.entityManager = entityManager;
        this.notificationEvents = notificationEvents;
    }

    NotificationServiceImpl(
            AuthApiService authApiService,
            NotificationRepository notificationRepository,
            EntityManager entityManager) {
        this(authApiService, notificationRepository, entityManager, new NotificationEvents());
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponseDto findMyNotifications(String authenticatedEmail, int limit) {
        UserInfo recipientInfo = authApiService.findUserByEmail(authenticatedEmail);
        int sanitizedLimit = sanitizeLimit(limit);
        long unreadCount = notificationRepository.countByRecipientUserIdAndReadAtIsNull(recipientInfo.userId());

        List<NotificationDto> notifications = unreadCount > sanitizedLimit
                ? notificationRepository.findUnreadDtosByRecipientUserIdOrderByCreatedAtDesc(
                        recipientInfo.userId(),
                        PageRequest.of(0, toPageSize(unreadCount)))
                : notificationRepository.findDtosByRecipientUserIdOrderByCreatedAtDesc(
                        recipientInfo.userId(),
                        PageRequest.of(0, sanitizedLimit));

        return new NotificationListResponseDto(notifications, unreadCount);
    }

    @Override
    @Transactional(readOnly = true)
    public SseEmitter openNotificationStream(String authenticatedEmail) {
        UserInfo recipientInfo = authApiService.findUserByEmail(authenticatedEmail);
        return notificationEvents.open(recipientInfo.userId());
    }

    @Override
    @Transactional
    public NotificationDto markNotificationAsRead(String authenticatedEmail, Long notificationId) {
        UserInfo recipientInfo = authApiService.findUserByEmail(authenticatedEmail);

        Notification notification = notificationRepository
                .findByIdAndRecipientUserId(notificationId, recipientInfo.userId())
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
            publishNotificationChange(recipientInfo.userId());
        }

        return toDto(notification);
    }

    @Override
    @Transactional
    public void markAllNotificationsAsRead(String authenticatedEmail) {
        UserInfo recipientInfo = authApiService.findUserByEmail(authenticatedEmail);
        int updatedNotifications = notificationRepository.markAllAsReadByRecipientUserId(
                recipientInfo.userId(),
                Instant.now());

        if (updatedNotifications > 0) {
            publishNotificationChange(recipientInfo.userId());
        }
    }

    @Override
    @Transactional
    public void createResearchGroupInvitationReceivedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long researchGroupId,
            String researchGroupName,
            Long invitationId) {
        createNotification(
                NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED,
                recipientUserId,
                actorUserId,
                researchGroupId,
                researchGroupName,
                invitationId,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Override
    @Transactional
    public void createResearchGroupInvitationAcceptedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long researchGroupId,
            String researchGroupName) {
        createNotification(
                NotificationType.RESEARCH_GROUP_INVITATION_ACCEPTED,
                recipientUserId,
                actorUserId,
                researchGroupId,
                researchGroupName,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Override
    @Transactional
    public void createResearchGroupInvitationDeclinedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long researchGroupId,
            String researchGroupName) {
        createNotification(
                NotificationType.RESEARCH_GROUP_INVITATION_DECLINED,
                recipientUserId,
                actorUserId,
                researchGroupId,
                researchGroupName,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Override
    @Transactional
    public void createResearchGroupMemberJoinedByCodeNotification(
            Long recipientUserId,
            Long actorUserId,
            Long researchGroupId,
            String researchGroupName) {
        createNotification(
                NotificationType.RESEARCH_GROUP_MEMBER_JOINED_BY_CODE,
                recipientUserId,
                actorUserId,
                researchGroupId,
                researchGroupName,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Override
    @Transactional
    public void createResearchGroupMemberRoleUpdatedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long researchGroupId,
            String researchGroupName) {
        createNotification(
                NotificationType.RESEARCH_GROUP_MEMBER_ROLE_UPDATED,
                recipientUserId,
                actorUserId,
                researchGroupId,
                researchGroupName,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Override
    @Transactional
    public void createResearchGroupMemberRemovedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long researchGroupId,
            String researchGroupName) {
        createNotification(
                NotificationType.RESEARCH_GROUP_MEMBER_REMOVED,
                recipientUserId,
                actorUserId,
                researchGroupId,
                researchGroupName,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Override
    @Transactional
    public void createProjectParticipantAssignedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long projectId,
            String projectName,
            Long researchGroupId,
            String researchGroupName) {
        createNotification(
                NotificationType.PROJECT_PARTICIPANT_ASSIGNED,
                recipientUserId,
                actorUserId,
                researchGroupId,
                researchGroupName,
                null,
                projectId,
                projectName,
                null,
                null,
                null,
                null);
    }

    @Override
    @Transactional
    public void createProjectParticipantUnassignedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long projectId,
            String projectName,
            Long researchGroupId,
            String researchGroupName) {
        createNotification(
                NotificationType.PROJECT_PARTICIPANT_UNASSIGNED,
                recipientUserId,
                actorUserId,
                researchGroupId,
                researchGroupName,
                null,
                projectId,
                projectName,
                null,
                null,
                null,
                null);
    }

    @Override
    @Transactional
    public void createProjectArchivedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long projectId,
            String projectName,
            Long researchGroupId,
            String researchGroupName) {
        createNotification(
                NotificationType.PROJECT_ARCHIVED,
                recipientUserId,
                actorUserId,
                researchGroupId,
                researchGroupName,
                null,
                projectId,
                projectName,
                null,
                null,
                null,
                null);
    }

    @Override
    @Transactional
    public void createProjectDeletedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long projectId,
            String projectName,
            Long researchGroupId,
            String researchGroupName) {
        createNotification(
                NotificationType.PROJECT_DELETED,
                recipientUserId,
                actorUserId,
                researchGroupId,
                researchGroupName,
                null,
                projectId,
                projectName,
                null,
                null,
                null,
                null);
    }

    @Override
    @Transactional
    public void createProjectAnnotationCompletedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long projectId,
            String projectName,
            Long researchGroupId,
            String researchGroupName) {
        if (notificationRepository.existsByRecipientUserIdAndActorUserIdAndTypeAndProjectId(
                recipientUserId,
                actorUserId,
                NotificationType.PROJECT_ANNOTATION_COMPLETED,
                projectId)) {
            return;
        }

        createNotification(
                NotificationType.PROJECT_ANNOTATION_COMPLETED,
                recipientUserId,
                actorUserId,
                researchGroupId,
                researchGroupName,
                null,
                projectId,
                projectName,
                null,
                null,
                null,
                null);
    }

    @Override
    @Transactional
    public void createProjectAnnotationWarningNotification(
            Long recipientUserId,
            Long actorUserId,
            Long projectId,
            String projectName,
            Long researchGroupId,
            String researchGroupName,
            Long datasetItemId,
            Integer datasetItemIndex,
            String datasetItemName,
            Integer annotationStepIndex) {
        createNotification(
                NotificationType.ANNOTATION_WARNING_MARKED,
                recipientUserId,
                actorUserId,
                researchGroupId,
                researchGroupName,
                null,
                projectId,
                projectName,
                datasetItemId,
                datasetItemIndex,
                datasetItemName,
                annotationStepIndex);
    }

    @Override
    @Transactional
    public void createProjectAnnotationWarningResolvedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long projectId,
            String projectName,
            Long researchGroupId,
            String researchGroupName,
            Long datasetItemId,
            Integer datasetItemIndex,
            String datasetItemName,
            Integer annotationStepIndex) {
        createNotification(
                NotificationType.ANNOTATION_WARNING_RESOLVED,
                recipientUserId,
                actorUserId,
                researchGroupId,
                researchGroupName,
                null,
                projectId,
                projectName,
                datasetItemId,
                datasetItemIndex,
                datasetItemName,
                annotationStepIndex);
    }

    @Override
    @Transactional
    public void createProjectAnnotationWarningClearedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long projectId,
            String projectName,
            Long researchGroupId,
            String researchGroupName,
            Long datasetItemId,
            Integer datasetItemIndex,
            String datasetItemName,
            Integer annotationStepIndex) {
        createNotification(
                NotificationType.ANNOTATION_WARNING_CLEARED,
                recipientUserId,
                actorUserId,
                researchGroupId,
                researchGroupName,
                null,
                projectId,
                projectName,
                datasetItemId,
                datasetItemIndex,
                datasetItemName,
                annotationStepIndex);
    }

    @Override
    @Transactional
    public void deleteNotificationsByProjectId(Long projectId) {
        List<Long> recipientUserIds = notificationRepository.findRecipientUserIdsByProjectId(projectId);
        long deletedNotifications = notificationRepository.deleteByProjectId(projectId);
        publishNotificationChangesIfNeeded(recipientUserIds, deletedNotifications);
    }

    @Override
    @Transactional
    public void deleteNotificationsByResearchGroupId(Long researchGroupId) {
        List<Long> recipientUserIds = notificationRepository.findRecipientUserIdsByResearchGroupId(researchGroupId);
        long deletedNotifications = notificationRepository.deleteByResearchGroupId(researchGroupId);
        publishNotificationChangesIfNeeded(recipientUserIds, deletedNotifications);
    }

    private NotificationDto toDto(Notification notification) {
        User actor = notification.getActorUser();
        String actorFullName = actor == null
                ? null
                : (actor.getFirstName() + " " + actor.getLastName()).trim();

        return new NotificationDto(
                notification.getId(),
                notification.getType(),
                notification.getReadAt() != null,
                notification.getCreatedAt(),
                actorFullName,
                notification.getResearchGroupId(),
                notification.getResearchGroupName(),
                notification.getInvitationId(),
                notification.getProjectId(),
                notification.getProjectName(),
                notification.getDatasetItemId(),
                notification.getDatasetItemIndex(),
                notification.getDatasetItemName(),
                notification.getAnnotationStepIndex());
    }

    private void createNotification(
            NotificationType type,
            Long recipientUserId,
            Long actorUserId,
            Long researchGroupId,
            String researchGroupName,
            Long invitationId,
            Long projectId,
            String projectName,
            Long datasetItemId,
            Integer datasetItemIndex,
            String datasetItemName,
            Integer annotationStepIndex) {
        Notification notification = new Notification();
        notification.setRecipientUser(getUserReference(recipientUserId));
        notification.setActorUser(actorUserId == null ? null : getUserReference(actorUserId));
        notification.setType(type);
        notification.setResearchGroupId(researchGroupId);
        notification.setResearchGroupName(researchGroupName);
        notification.setInvitationId(invitationId);
        notification.setProjectId(projectId);
        notification.setProjectName(projectName);
        notification.setDatasetItemId(datasetItemId);
        notification.setDatasetItemIndex(datasetItemIndex);
        notification.setDatasetItemName(datasetItemName);
        notification.setAnnotationStepIndex(annotationStepIndex);

        notificationRepository.save(notification);
        publishNotificationChange(recipientUserId);
    }

    private void publishNotificationChange(Long userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notificationEvents.publish(userId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationEvents.publish(userId);
            }
        });
    }

    private void publishNotificationChangesIfNeeded(List<Long> recipientUserIds, long changedNotifications) {
        if (changedNotifications <= 0) {
            return;
        }

        for (Long recipientUserId : recipientUserIds) {
            publishNotificationChange(recipientUserId);
        }
    }

    private int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }

    private int toPageSize(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    /**
     * Creates a JPA proxy reference for User without loading the entity.
     * Used only to satisfy @ManyToOne FK relationship on Notification.
     */
    private User getUserReference(Long userId) {
        return entityManager.getReference(User.class, userId);
    }
}
