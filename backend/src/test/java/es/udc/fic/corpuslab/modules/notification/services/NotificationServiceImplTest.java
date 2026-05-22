package es.udc.fic.corpuslab.modules.notification.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.notification.dtos.NotificationDto;
import es.udc.fic.corpuslab.modules.notification.dtos.NotificationListResponseDto;
import es.udc.fic.corpuslab.modules.notification.entities.Notification;
import es.udc.fic.corpuslab.modules.notification.enums.NotificationType;
import es.udc.fic.corpuslab.modules.notification.exceptions.NotificationNotFoundException;
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private AuthApiService authApiService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EntityManager entityManager;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(authApiService, notificationRepository, entityManager);
    }

    @Test
    void findMyNotificationsShouldReturnMappedNotificationsAndUnreadCount() {
        User recipient = UserTestBuilder.validUser().withEmail("reader@example.com").build();
        setField(recipient, "id", 11L);

        User actor = UserTestBuilder.validUser().withFirstName("Ada").withLastName("Lovelace").build();
        setField(actor, "id", 12L);

        when(authApiService.findUserByEmail("reader@example.com"))
                .thenReturn(new UserInfo(11L, "reader@example.com", "Reader", "User"));

        Notification unread = new Notification();
        unread.setRecipientUser(recipient);
        unread.setActorUser(actor);
        unread.setType(NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED);
        unread.setResearchGroupId(100L);
        unread.setResearchGroupName("NLP Group");
        unread.setInvitationId(500L);
        setField(unread, "id", 900L);
        setField(unread, "createdAt", Instant.parse("2026-04-04T10:00:00Z"));

        Notification alreadyRead = new Notification();
        alreadyRead.setRecipientUser(recipient);
        alreadyRead.setActorUser(actor);
        alreadyRead.setType(NotificationType.RESEARCH_GROUP_INVITATION_ACCEPTED);
        alreadyRead.setResearchGroupId(101L);
        alreadyRead.setResearchGroupName("Corpus Group");
        alreadyRead.setReadAt(Instant.parse("2026-04-04T11:00:00Z"));
        setField(alreadyRead, "id", 901L);
        setField(alreadyRead, "createdAt", Instant.parse("2026-04-04T09:00:00Z"));

        when(notificationRepository.findDtosByRecipientUserIdOrderByCreatedAtDesc(eq(11L), any(Pageable.class)))
                .thenReturn(List.of(toDto(unread), toDto(alreadyRead)));
        when(notificationRepository.countByRecipientUserIdAndReadAtIsNull(11L)).thenReturn(1L);

        NotificationListResponseDto result = notificationService.findMyNotifications("reader@example.com", 0);

        assertThat(result.unreadCount()).isEqualTo(1L);
        assertThat(result.notifications()).hasSize(2);

        NotificationDto first = result.notifications().get(0);
        assertThat(first.id()).isEqualTo(900L);
        assertThat(first.type()).isEqualTo(NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED);
        assertThat(first.read()).isFalse();
        assertThat(first.actorFullName()).isEqualTo("Ada Lovelace");
        assertThat(first.researchGroupId()).isEqualTo(100L);
        assertThat(first.researchGroupName()).isEqualTo("NLP Group");
        assertThat(first.invitationId()).isEqualTo(500L);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findDtosByRecipientUserIdOrderByCreatedAtDesc(eq(11L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void findMyNotificationsShouldCapLimitAtFifty() {
        when(authApiService.findUserByEmail("reader@example.com"))
                .thenReturn(new UserInfo(11L, "reader@example.com", "Reader", "User"));
        when(notificationRepository.findDtosByRecipientUserIdOrderByCreatedAtDesc(eq(11L), any(Pageable.class)))
                .thenReturn(List.of());
        when(notificationRepository.countByRecipientUserIdAndReadAtIsNull(11L)).thenReturn(0L);

        notificationService.findMyNotifications("reader@example.com", 100);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findDtosByRecipientUserIdOrderByCreatedAtDesc(eq(11L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void findMyNotificationsShouldReturnAllUnreadWhenUnreadCountExceedsLimit() {
        User recipient = UserTestBuilder.validUser().withEmail("reader@example.com").build();
        setField(recipient, "id", 11L);

        Notification firstUnread = new Notification();
        firstUnread.setRecipientUser(recipient);
        firstUnread.setType(NotificationType.ANNOTATION_WARNING_MARKED);
        setField(firstUnread, "id", 100L);
        setField(firstUnread, "createdAt", Instant.parse("2026-04-04T12:00:00Z"));

        Notification secondUnread = new Notification();
        secondUnread.setRecipientUser(recipient);
        secondUnread.setType(NotificationType.ANNOTATION_WARNING_CLEARED);
        setField(secondUnread, "id", 99L);
        setField(secondUnread, "createdAt", Instant.parse("2026-04-04T11:00:00Z"));

        when(authApiService.findUserByEmail("reader@example.com"))
                .thenReturn(new UserInfo(11L, "reader@example.com", "Reader", "User"));
        when(notificationRepository.countByRecipientUserIdAndReadAtIsNull(11L)).thenReturn(11L);
        when(notificationRepository.findUnreadDtosByRecipientUserIdOrderByCreatedAtDesc(eq(11L), any(Pageable.class)))
                .thenReturn(List.of(toDto(firstUnread), toDto(secondUnread)));

        NotificationListResponseDto result = notificationService.findMyNotifications("reader@example.com", 10);

        assertThat(result.unreadCount()).isEqualTo(11L);
        assertThat(result.notifications()).extracting(NotificationDto::id).containsExactly(100L, 99L);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository)
                .findUnreadDtosByRecipientUserIdOrderByCreatedAtDesc(eq(11L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(11);
        verify(notificationRepository, never())
                .findDtosByRecipientUserIdOrderByCreatedAtDesc(eq(11L), any(Pageable.class));
    }

    @Test
    void markNotificationAsReadShouldPersistWhenUnread() {
        User recipient = UserTestBuilder.validUser().withEmail("reader@example.com").build();
        setField(recipient, "id", 11L);

        when(authApiService.findUserByEmail("reader@example.com"))
                .thenReturn(new UserInfo(11L, "reader@example.com", "Reader", "User"));

        Notification notification = new Notification();
        notification.setRecipientUser(recipient);
        notification.setType(NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED);
        setField(notification, "id", 33L);
        setField(notification, "createdAt", Instant.parse("2026-04-04T10:00:00Z"));

        when(notificationRepository.findByIdAndRecipientUserId(33L, 11L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationDto result = notificationService.markNotificationAsRead("reader@example.com", 33L);

        assertThat(result.read()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markNotificationAsReadShouldNotPersistAgainWhenAlreadyRead() {
        User recipient = UserTestBuilder.validUser().withEmail("reader@example.com").build();
        setField(recipient, "id", 11L);

        when(authApiService.findUserByEmail("reader@example.com"))
                .thenReturn(new UserInfo(11L, "reader@example.com", "Reader", "User"));

        Notification notification = new Notification();
        notification.setRecipientUser(recipient);
        notification.setType(NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED);
        notification.setReadAt(Instant.parse("2026-04-04T11:00:00Z"));
        setField(notification, "id", 33L);
        setField(notification, "createdAt", Instant.parse("2026-04-04T10:00:00Z"));

        when(notificationRepository.findByIdAndRecipientUserId(33L, 11L)).thenReturn(Optional.of(notification));

        NotificationDto result = notificationService.markNotificationAsRead("reader@example.com", 33L);

        assertThat(result.read()).isTrue();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void markNotificationAsReadShouldThrowWhenNotificationDoesNotBelongToUser() {
        when(authApiService.findUserByEmail("reader@example.com"))
                .thenReturn(new UserInfo(11L, "reader@example.com", "Reader", "User"));
        when(notificationRepository.findByIdAndRecipientUserId(99L, 11L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markNotificationAsRead("reader@example.com", 99L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessage("Notification not found: 99");
    }

    @Test
    void markAllNotificationsAsReadShouldDelegateToRepository() {
        when(authApiService.findUserByEmail("reader@example.com"))
                .thenReturn(new UserInfo(11L, "reader@example.com", "Reader", "User"));

        notificationService.markAllNotificationsAsRead("reader@example.com");

        verify(notificationRepository).markAllAsReadByRecipientUserId(eq(11L), any(Instant.class));
    }

    @Test
    void createInvitationReceivedNotificationShouldSaveExpectedData() {
        User recipientRef = UserTestBuilder.validUser().build();
        setField(recipientRef, "id", 10L);
        User actorRef = UserTestBuilder.validUser().build();
        setField(actorRef, "id", 20L);

        when(entityManager.getReference(User.class, 10L)).thenReturn(recipientRef);
        when(entityManager.getReference(User.class, 20L)).thenReturn(actorRef);

        notificationService.createResearchGroupInvitationReceivedNotification(
                10L, 20L, 201L, "NLP Group", 9000L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getRecipientUser()).isEqualTo(recipientRef);
        assertThat(saved.getActorUser()).isEqualTo(actorRef);
        assertThat(saved.getType()).isEqualTo(NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED);
        assertThat(saved.getResearchGroupId()).isEqualTo(201L);
        assertThat(saved.getResearchGroupName()).isEqualTo("NLP Group");
        assertThat(saved.getInvitationId()).isEqualTo(9000L);
    }

    @Test
    void createInvitationAcceptedNotificationShouldSaveExpectedData() {
        User recipientRef = UserTestBuilder.validUser().build();
        setField(recipientRef, "id", 10L);
        User actorRef = UserTestBuilder.validUser().build();
        setField(actorRef, "id", 20L);

        when(entityManager.getReference(User.class, 10L)).thenReturn(recipientRef);
        when(entityManager.getReference(User.class, 20L)).thenReturn(actorRef);

        notificationService.createResearchGroupInvitationAcceptedNotification(
                10L, 20L, 301L, "Corpus Group");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getRecipientUser()).isEqualTo(recipientRef);
        assertThat(saved.getActorUser()).isEqualTo(actorRef);
        assertThat(saved.getType()).isEqualTo(NotificationType.RESEARCH_GROUP_INVITATION_ACCEPTED);
        assertThat(saved.getResearchGroupId()).isEqualTo(301L);
        assertThat(saved.getResearchGroupName()).isEqualTo("Corpus Group");
        assertThat(saved.getInvitationId()).isNull();
    }

    @Test
    void createProjectParticipantAssignedNotificationShouldSaveExpectedData() {
        User recipientRef = UserTestBuilder.validUser().build();
        setField(recipientRef, "id", 30L);
        User actorRef = UserTestBuilder.validUser().build();
        setField(actorRef, "id", 40L);

        when(entityManager.getReference(User.class, 30L)).thenReturn(recipientRef);
        when(entityManager.getReference(User.class, 40L)).thenReturn(actorRef);

        notificationService.createProjectParticipantAssignedNotification(
                30L, 40L, 501L, "Annotation Project", 401L, "Project Group");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getRecipientUser()).isEqualTo(recipientRef);
        assertThat(saved.getActorUser()).isEqualTo(actorRef);
        assertThat(saved.getType()).isEqualTo(NotificationType.PROJECT_PARTICIPANT_ASSIGNED);
        assertThat(saved.getProjectId()).isEqualTo(501L);
        assertThat(saved.getProjectName()).isEqualTo("Annotation Project");
        assertThat(saved.getResearchGroupId()).isEqualTo(401L);
        assertThat(saved.getResearchGroupName()).isEqualTo("Project Group");
    }

    @Test
    void createProjectAnnotationCompletedNotificationShouldSaveWhenNoPreviousNotificationExists() {
        User recipientRef = UserTestBuilder.validUser().build();
        setField(recipientRef, "id", 41L);
        User actorRef = UserTestBuilder.validUser().build();
        setField(actorRef, "id", 42L);

        when(entityManager.getReference(User.class, 41L)).thenReturn(recipientRef);
        when(entityManager.getReference(User.class, 42L)).thenReturn(actorRef);
        when(notificationRepository.existsByRecipientUserIdAndActorUserIdAndTypeAndProjectId(
                41L, 42L, NotificationType.PROJECT_ANNOTATION_COMPLETED, 701L)).thenReturn(false);

        notificationService.createProjectAnnotationCompletedNotification(
                41L, 42L, 701L, "Completion Project", 601L, "Completion Group");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(NotificationType.PROJECT_ANNOTATION_COMPLETED);
        assertThat(saved.getProjectId()).isEqualTo(701L);
        assertThat(saved.getProjectName()).isEqualTo("Completion Project");
    }

    @Test
    void createProjectAnnotationCompletedNotificationShouldSkipWhenDuplicateExists() {
        when(notificationRepository.existsByRecipientUserIdAndActorUserIdAndTypeAndProjectId(
                41L, 42L, NotificationType.PROJECT_ANNOTATION_COMPLETED, 701L)).thenReturn(true);

        notificationService.createProjectAnnotationCompletedNotification(
                41L, 42L, 701L, "Completion Project", 601L, "Completion Group");

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void findMyNotificationsShouldIncludeProjectFieldsInDto() {
        User actor = UserTestBuilder.validUser().withFirstName("Grace").withLastName("Hopper").build();
        setField(actor, "id", 22L);

        when(authApiService.findUserByEmail("reader.project@example.com"))
                .thenReturn(new UserInfo(21L, "reader.project@example.com", "Reader", "Project"));

        Notification notification = new Notification();
        notification.setActorUser(actor);
        notification.setType(NotificationType.PROJECT_PARTICIPANT_ASSIGNED);
        notification.setResearchGroupId(801L);
        notification.setResearchGroupName("Project RG");
        notification.setProjectId(901L);
        notification.setProjectName("Project X");
        setField(notification, "id", 1001L);
        setField(notification, "createdAt", Instant.parse("2026-04-04T10:00:00Z"));

        when(notificationRepository.findDtosByRecipientUserIdOrderByCreatedAtDesc(eq(21L), any(Pageable.class)))
                .thenReturn(List.of(toDto(notification)));
        when(notificationRepository.countByRecipientUserIdAndReadAtIsNull(21L)).thenReturn(1L);

        NotificationListResponseDto result = notificationService.findMyNotifications("reader.project@example.com", 5);

        assertThat(result.notifications()).hasSize(1);
        NotificationDto dto = result.notifications().get(0);
        assertThat(dto.projectId()).isEqualTo(901L);
        assertThat(dto.projectName()).isEqualTo("Project X");
        assertThat(dto.actorFullName()).isEqualTo("Grace Hopper");
    }

    @Test
    void findMyNotificationsShouldThrowWhenUserEmailDoesNotExist() {
        when(authApiService.findUserByEmail("missing@example.com"))
                .thenThrow(new EmailNotFoundException("missing@example.com"));

        assertThatThrownBy(() -> notificationService.findMyNotifications("missing@example.com", 20))
                .isInstanceOf(EmailNotFoundException.class)
                .hasMessage("No account found for email: missing@example.com");
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to set field " + fieldName, ex);
        }
    }

    private static NotificationDto toDto(Notification notification) {
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
}
