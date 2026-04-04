package es.udc.fic.corpuslab.modules.notification.services;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.auth.utils.EmailNormalizer;
import es.udc.fic.corpuslab.modules.notification.dtos.NotificationDto;
import es.udc.fic.corpuslab.modules.notification.dtos.NotificationListResponseDto;
import es.udc.fic.corpuslab.modules.notification.entities.Notification;
import es.udc.fic.corpuslab.modules.notification.enums.NotificationType;
import es.udc.fic.corpuslab.modules.notification.exceptions.NotificationNotFoundException;
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(UserRepository userRepository, NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponseDto findMyNotifications(String authenticatedEmail, int limit) {
        User recipient = findUserByEmail(authenticatedEmail);
        int sanitizedLimit = sanitizeLimit(limit);

        List<NotificationDto> notifications = notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(recipient.getId(), PageRequest.of(0, sanitizedLimit))
                .stream()
                .map(this::toDto)
                .toList();

        long unreadCount = notificationRepository.countByRecipientUserIdAndReadAtIsNull(recipient.getId());

        return new NotificationListResponseDto(notifications, unreadCount);
    }

    @Override
    @Transactional
    public NotificationDto markNotificationAsRead(String authenticatedEmail, Long notificationId) {
        User recipient = findUserByEmail(authenticatedEmail);

        Notification notification = notificationRepository.findByIdAndRecipientUserId(notificationId, recipient.getId())
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }

        return toDto(notification);
    }

    @Override
    @Transactional
    public void markAllNotificationsAsRead(String authenticatedEmail) {
        User recipient = findUserByEmail(authenticatedEmail);
        notificationRepository.markAllAsReadByRecipientUserId(recipient.getId(), Instant.now());
    }

    @Override
    @Transactional
    public void createResearchGroupInvitationReceivedNotification(
            User recipient,
            User inviter,
            ResearchGroup researchGroup,
            Long invitationId) {
        Notification notification = new Notification();
        notification.setRecipientUser(recipient);
        notification.setActorUser(inviter);
        notification.setType(NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED);
        notification.setResearchGroupId(researchGroup.getId());
        notification.setResearchGroupName(researchGroup.getName());
        notification.setInvitationId(invitationId);

        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void createResearchGroupInvitationAcceptedNotification(User recipient, User actor,
            ResearchGroup researchGroup) {
        Notification notification = new Notification();
        notification.setRecipientUser(recipient);
        notification.setActorUser(actor);
        notification.setType(NotificationType.RESEARCH_GROUP_INVITATION_ACCEPTED);
        notification.setResearchGroupId(researchGroup.getId());
        notification.setResearchGroupName(researchGroup.getName());

        notificationRepository.save(notification);
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
                notification.getInvitationId());
    }

    private User findUserByEmail(String authenticatedEmail) {
        String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);
        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));
    }

    private int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }
}
