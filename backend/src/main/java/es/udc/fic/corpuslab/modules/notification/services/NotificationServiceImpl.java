package es.udc.fic.corpuslab.modules.notification.services;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private static final int DEFAULT_LIMIT = 20;
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

        List<NotificationDto> notifications = notificationRepository
                .findDtosByRecipientUserIdOrderByCreatedAtDesc(recipientInfo.userId(), PageRequest.of(0, sanitizedLimit));

        long unreadCount = notificationRepository.countByRecipientUserIdAndReadAtIsNull(recipientInfo.userId());

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
            notificationEvents.publish(recipientInfo.userId());
        }

        return toDto(notification);
    }

    @Override
    @Transactional
    public void markAllNotificationsAsRead(String authenticatedEmail) {
        UserInfo recipientInfo = authApiService.findUserByEmail(authenticatedEmail);
        notificationRepository.markAllAsReadByRecipientUserId(recipientInfo.userId(), Instant.now());
        notificationEvents.publish(recipientInfo.userId());
    }

    @Override
    @Transactional
    public void createResearchGroupInvitationReceivedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long researchGroupId,
            String researchGroupName,
            Long invitationId) {
        Notification notification = new Notification();
        notification.setRecipientUser(getUserReference(recipientUserId));
        notification.setActorUser(getUserReference(actorUserId));
        notification.setType(NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED);
        notification.setResearchGroupId(researchGroupId);
        notification.setResearchGroupName(researchGroupName);
        notification.setInvitationId(invitationId);

        notificationRepository.save(notification);
        notificationEvents.publish(recipientUserId);
    }

    @Override
    @Transactional
    public void createResearchGroupInvitationAcceptedNotification(
            Long recipientUserId,
            Long actorUserId,
            Long researchGroupId,
            String researchGroupName) {
        Notification notification = new Notification();
        notification.setRecipientUser(getUserReference(recipientUserId));
        notification.setActorUser(getUserReference(actorUserId));
        notification.setType(NotificationType.RESEARCH_GROUP_INVITATION_ACCEPTED);
        notification.setResearchGroupId(researchGroupId);
        notification.setResearchGroupName(researchGroupName);

        notificationRepository.save(notification);
        notificationEvents.publish(recipientUserId);
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
        Notification notification = new Notification();
        notification.setRecipientUser(getUserReference(recipientUserId));
        notification.setActorUser(getUserReference(actorUserId));
        notification.setType(NotificationType.PROJECT_PARTICIPANT_ASSIGNED);
        notification.setProjectId(projectId);
        notification.setProjectName(projectName);
        notification.setResearchGroupId(researchGroupId);
        notification.setResearchGroupName(researchGroupName);

        notificationRepository.save(notification);
        notificationEvents.publish(recipientUserId);
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

        Notification notification = new Notification();
        notification.setRecipientUser(getUserReference(recipientUserId));
        notification.setActorUser(getUserReference(actorUserId));
        notification.setType(NotificationType.PROJECT_ANNOTATION_COMPLETED);
        notification.setProjectId(projectId);
        notification.setProjectName(projectName);
        notification.setResearchGroupId(researchGroupId);
        notification.setResearchGroupName(researchGroupName);

        notificationRepository.save(notification);
        notificationEvents.publish(recipientUserId);
    }

    @Override
    @Transactional
    public void createProjectAnnotationWarningNotification(
            Long recipientUserId,
            Long actorUserId,
            Long projectId,
            String projectName,
            Long researchGroupId,
            String researchGroupName) {
        Notification notification = new Notification();
        notification.setRecipientUser(getUserReference(recipientUserId));
        notification.setActorUser(getUserReference(actorUserId));
        notification.setType(NotificationType.ANNOTATION_WARNING_MARKED);
        notification.setProjectId(projectId);
        notification.setProjectName(projectName);
        notification.setResearchGroupId(researchGroupId);
        notification.setResearchGroupName(researchGroupName);

        notificationRepository.save(notification);
        notificationEvents.publish(recipientUserId);
    }

    @Override
    @Transactional
    public void deleteNotificationsByProjectId(Long projectId) {
        notificationRepository.deleteByProjectId(projectId);
    }

    @Override
    @Transactional
    public void deleteNotificationsByResearchGroupId(Long researchGroupId) {
        notificationRepository.deleteByResearchGroupId(researchGroupId);
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
                notification.getProjectName());
    }

    private int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * Creates a JPA proxy reference for User without loading the entity.
     * Used only to satisfy @ManyToOne FK relationship on Notification.
     */
    private User getUserReference(Long userId) {
        return entityManager.getReference(User.class, userId);
    }
}
