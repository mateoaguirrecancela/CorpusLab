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

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.notification.dtos.NotificationDto;
import es.udc.fic.corpuslab.modules.notification.dtos.NotificationListResponseDto;
import es.udc.fic.corpuslab.modules.notification.entities.Notification;
import es.udc.fic.corpuslab.modules.notification.enums.NotificationType;
import es.udc.fic.corpuslab.modules.notification.exceptions.NotificationNotFoundException;
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupTestBuilder;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(userRepository, notificationRepository);
    }

    @Test
    void findMyNotificationsShouldReturnMappedNotificationsAndUnreadCount() {
        User recipient = UserTestBuilder.validUser().withEmail("reader@example.com").build();
        setField(recipient, "id", 11L);

        User actor = UserTestBuilder.validUser().withFirstName("Ada").withLastName("Lovelace").build();
        setField(actor, "id", 12L);

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

        when(userRepository.findByEmailIgnoreCase("reader@example.com"))
                .thenReturn(Optional.of(recipient));
        when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(eq(11L), any(Pageable.class)))
                .thenReturn(List.of(unread, alreadyRead));
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
        verify(notificationRepository).findByRecipientUserIdOrderByCreatedAtDesc(eq(11L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void findMyNotificationsShouldCapLimitAtFifty() {
        User recipient = UserTestBuilder.validUser().withEmail("reader@example.com").build();
        setField(recipient, "id", 11L);

        when(userRepository.findByEmailIgnoreCase("reader@example.com")).thenReturn(Optional.of(recipient));
        when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(eq(11L), any(Pageable.class)))
                .thenReturn(List.of());
        when(notificationRepository.countByRecipientUserIdAndReadAtIsNull(11L)).thenReturn(0L);

        notificationService.findMyNotifications("reader@example.com", 100);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findByRecipientUserIdOrderByCreatedAtDesc(eq(11L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void markNotificationAsReadShouldPersistWhenUnread() {
        User recipient = UserTestBuilder.validUser().withEmail("reader@example.com").build();
        setField(recipient, "id", 11L);

        Notification notification = new Notification();
        notification.setRecipientUser(recipient);
        notification.setType(NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED);
        setField(notification, "id", 33L);
        setField(notification, "createdAt", Instant.parse("2026-04-04T10:00:00Z"));

        when(userRepository.findByEmailIgnoreCase("reader@example.com")).thenReturn(Optional.of(recipient));
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

        Notification notification = new Notification();
        notification.setRecipientUser(recipient);
        notification.setType(NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED);
        notification.setReadAt(Instant.parse("2026-04-04T11:00:00Z"));
        setField(notification, "id", 33L);
        setField(notification, "createdAt", Instant.parse("2026-04-04T10:00:00Z"));

        when(userRepository.findByEmailIgnoreCase("reader@example.com")).thenReturn(Optional.of(recipient));
        when(notificationRepository.findByIdAndRecipientUserId(33L, 11L)).thenReturn(Optional.of(notification));

        NotificationDto result = notificationService.markNotificationAsRead("reader@example.com", 33L);

        assertThat(result.read()).isTrue();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void markNotificationAsReadShouldThrowWhenNotificationDoesNotBelongToUser() {
        User recipient = UserTestBuilder.validUser().withEmail("reader@example.com").build();
        setField(recipient, "id", 11L);

        when(userRepository.findByEmailIgnoreCase("reader@example.com")).thenReturn(Optional.of(recipient));
        when(notificationRepository.findByIdAndRecipientUserId(99L, 11L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markNotificationAsRead("reader@example.com", 99L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessage("Notification not found: 99");
    }

    @Test
    void markAllNotificationsAsReadShouldDelegateToRepository() {
        User recipient = UserTestBuilder.validUser().withEmail("reader@example.com").build();
        setField(recipient, "id", 11L);

        when(userRepository.findByEmailIgnoreCase("reader@example.com")).thenReturn(Optional.of(recipient));

        notificationService.markAllNotificationsAsRead("reader@example.com");

        verify(notificationRepository).markAllAsReadByRecipientUserId(eq(11L), any(Instant.class));
    }

    @Test
    void createInvitationReceivedNotificationShouldSaveExpectedData() {
        User recipient = UserTestBuilder.validUser().withEmail("recipient@example.com").build();
        User inviter = UserTestBuilder.validUser().withEmail("inviter@example.com").build();

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("NLP Group").build();
        setField(group, "id", 201L);

        notificationService.createResearchGroupInvitationReceivedNotification(recipient, inviter, group, 9000L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getRecipientUser()).isEqualTo(recipient);
        assertThat(saved.getActorUser()).isEqualTo(inviter);
        assertThat(saved.getType()).isEqualTo(NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED);
        assertThat(saved.getResearchGroupId()).isEqualTo(201L);
        assertThat(saved.getResearchGroupName()).isEqualTo("NLP Group");
        assertThat(saved.getInvitationId()).isEqualTo(9000L);
    }

    @Test
    void createInvitationAcceptedNotificationShouldSaveExpectedData() {
        User recipient = UserTestBuilder.validUser().withEmail("owner@example.com").build();
        User actor = UserTestBuilder.validUser().withEmail("member@example.com").build();

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("Corpus Group").build();
        setField(group, "id", 301L);

        notificationService.createResearchGroupInvitationAcceptedNotification(recipient, actor, group);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getRecipientUser()).isEqualTo(recipient);
        assertThat(saved.getActorUser()).isEqualTo(actor);
        assertThat(saved.getType()).isEqualTo(NotificationType.RESEARCH_GROUP_INVITATION_ACCEPTED);
        assertThat(saved.getResearchGroupId()).isEqualTo(301L);
        assertThat(saved.getResearchGroupName()).isEqualTo("Corpus Group");
        assertThat(saved.getInvitationId()).isNull();
    }

    @Test
    void createProjectParticipantAssignedNotificationShouldSaveExpectedData() {
        User recipient = UserTestBuilder.validUser().withEmail("assignee@example.com").build();
        User actor = UserTestBuilder.validUser().withEmail("owner@example.com").build();

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("Project Group").build();
        setField(group, "id", 401L);

        Project project = new Project();
        project.setResearchGroup(group);
        project.setName("Annotation Project");
        setField(project, "id", 501L);

        notificationService.createProjectParticipantAssignedNotification(recipient, actor, project);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getRecipientUser()).isEqualTo(recipient);
        assertThat(saved.getActorUser()).isEqualTo(actor);
        assertThat(saved.getType()).isEqualTo(NotificationType.PROJECT_PARTICIPANT_ASSIGNED);
        assertThat(saved.getProjectId()).isEqualTo(501L);
        assertThat(saved.getProjectName()).isEqualTo("Annotation Project");
        assertThat(saved.getResearchGroupId()).isEqualTo(401L);
        assertThat(saved.getResearchGroupName()).isEqualTo("Project Group");
    }

    @Test
    void createProjectAnnotationCompletedNotificationShouldSaveWhenNoPreviousNotificationExists() {
        User recipient = UserTestBuilder.validUser().withEmail("recipient@example.com").build();
        setField(recipient, "id", 41L);

        User actor = UserTestBuilder.validUser().withEmail("actor@example.com").build();
        setField(actor, "id", 42L);

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("Completion Group").build();
        setField(group, "id", 601L);

        Project project = new Project();
        project.setResearchGroup(group);
        project.setName("Completion Project");
        setField(project, "id", 701L);

        when(notificationRepository.existsByRecipientUserIdAndActorUserIdAndTypeAndProjectId(
                41L,
                42L,
                NotificationType.PROJECT_ANNOTATION_COMPLETED,
                701L)).thenReturn(false);

        notificationService.createProjectAnnotationCompletedNotification(recipient, actor, project);

        verify(notificationRepository).existsByRecipientUserIdAndActorUserIdAndTypeAndProjectId(
                41L,
                42L,
                NotificationType.PROJECT_ANNOTATION_COMPLETED,
                701L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getRecipientUser()).isEqualTo(recipient);
        assertThat(saved.getActorUser()).isEqualTo(actor);
        assertThat(saved.getType()).isEqualTo(NotificationType.PROJECT_ANNOTATION_COMPLETED);
        assertThat(saved.getProjectId()).isEqualTo(701L);
        assertThat(saved.getProjectName()).isEqualTo("Completion Project");
        assertThat(saved.getResearchGroupId()).isEqualTo(601L);
        assertThat(saved.getResearchGroupName()).isEqualTo("Completion Group");
    }

    @Test
    void createProjectAnnotationCompletedNotificationShouldSkipWhenDuplicateExists() {
        User recipient = UserTestBuilder.validUser().withEmail("recipient@example.com").build();
        setField(recipient, "id", 41L);

        User actor = UserTestBuilder.validUser().withEmail("actor@example.com").build();
        setField(actor, "id", 42L);

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("Completion Group").build();
        setField(group, "id", 601L);

        Project project = new Project();
        project.setResearchGroup(group);
        project.setName("Completion Project");
        setField(project, "id", 701L);

        when(notificationRepository.existsByRecipientUserIdAndActorUserIdAndTypeAndProjectId(
                41L,
                42L,
                NotificationType.PROJECT_ANNOTATION_COMPLETED,
                701L)).thenReturn(true);

        notificationService.createProjectAnnotationCompletedNotification(recipient, actor, project);

        verify(notificationRepository).existsByRecipientUserIdAndActorUserIdAndTypeAndProjectId(
                41L,
                42L,
                NotificationType.PROJECT_ANNOTATION_COMPLETED,
                701L);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void findMyNotificationsShouldIncludeProjectFieldsInDto() {
        User recipient = UserTestBuilder.validUser().withEmail("reader.project@example.com").build();
        setField(recipient, "id", 21L);

        User actor = UserTestBuilder.validUser().withFirstName("Grace").withLastName("Hopper").build();
        setField(actor, "id", 22L);

        Notification notification = new Notification();
        notification.setRecipientUser(recipient);
        notification.setActorUser(actor);
        notification.setType(NotificationType.PROJECT_PARTICIPANT_ASSIGNED);
        notification.setResearchGroupId(801L);
        notification.setResearchGroupName("Project RG");
        notification.setProjectId(901L);
        notification.setProjectName("Project X");
        setField(notification, "id", 1001L);
        setField(notification, "createdAt", Instant.parse("2026-04-04T10:00:00Z"));

        when(userRepository.findByEmailIgnoreCase("reader.project@example.com"))
                .thenReturn(Optional.of(recipient));
        when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(eq(21L), any(Pageable.class)))
                .thenReturn(List.of(notification));
        when(notificationRepository.countByRecipientUserIdAndReadAtIsNull(21L)).thenReturn(1L);

        NotificationListResponseDto result = notificationService.findMyNotifications("reader.project@example.com", 5);

        assertThat(result.notifications()).hasSize(1);
        NotificationDto dto = result.notifications().get(0);
        assertThat(dto.projectId()).isEqualTo(901L);
        assertThat(dto.projectName()).isEqualTo("Project X");
        assertThat(dto.actorFullName()).isEqualTo("Grace Hopper");
        assertThat(dto.researchGroupId()).isEqualTo(801L);
        assertThat(dto.researchGroupName()).isEqualTo("Project RG");
    }

    @Test
    void findMyNotificationsShouldThrowWhenUserEmailDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

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
}
