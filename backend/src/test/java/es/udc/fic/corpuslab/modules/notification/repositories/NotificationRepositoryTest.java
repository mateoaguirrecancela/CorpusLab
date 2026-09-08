package es.udc.fic.corpuslab.modules.notification.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.notification.dtos.NotificationDto;
import es.udc.fic.corpuslab.modules.notification.entities.Notification;
import es.udc.fic.corpuslab.modules.notification.enums.NotificationType;

@DataJpaTest
@ActiveProfiles("test")
class NotificationRepositoryTest {

        @Autowired
        private NotificationRepository notificationRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private EntityManager entityManager;

        @Test
        void findDtosByRecipientUserIdOrderByCreatedAtDescShouldReturnLatestFirstWithLimit() {
                User recipient = userRepository
                                .save(UserTestBuilder.validUser().withEmail("recipient@example.com").build());
                User actor = userRepository.save(UserTestBuilder.validUser().withEmail("actor@example.com").build());

                Notification oldest = saveNotification(recipient, actor,
                                NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED,
                                Instant.parse("2026-04-04T08:00:00Z"), null);
                Notification middle = saveNotification(recipient, actor,
                                NotificationType.RESEARCH_GROUP_INVITATION_ACCEPTED,
                                Instant.parse("2026-04-04T09:00:00Z"), Instant.parse("2026-04-04T09:30:00Z"));
                Notification latest = saveNotification(recipient, actor,
                                NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED,
                                Instant.parse("2026-04-04T10:00:00Z"), null);

                List<NotificationDto> notifications = notificationRepository
                                .findDtosByRecipientUserIdOrderByCreatedAtDesc(recipient.getId(),
                                                PageRequest.of(0, 2));

                assertThat(notifications).hasSize(2);
                assertThat(notifications.get(0).id()).isEqualTo(latest.getId());
                assertThat(notifications.get(1).id()).isEqualTo(middle.getId());
                assertThat(notifications).extracting(NotificationDto::id).doesNotContain(oldest.getId());
        }

        @Test
        void countByRecipientUserIdAndReadAtIsNullShouldOnlyCountUnread() {
                User recipient = userRepository
                                .save(UserTestBuilder.validUser().withEmail("recipient2@example.com").build());
                User actor = userRepository.save(UserTestBuilder.validUser().withEmail("actor2@example.com").build());

                saveNotification(recipient, actor, NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED,
                                Instant.parse("2026-04-04T08:00:00Z"), null);
                saveNotification(recipient, actor, NotificationType.RESEARCH_GROUP_INVITATION_ACCEPTED,
                                Instant.parse("2026-04-04T09:00:00Z"), Instant.parse("2026-04-04T09:30:00Z"));
                saveNotification(recipient, actor, NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED,
                                Instant.parse("2026-04-04T10:00:00Z"), null);

                long unreadCount = notificationRepository.countByRecipientUserIdAndReadAtIsNull(recipient.getId());

                assertThat(unreadCount).isEqualTo(2L);
        }

        @Test
        void findByIdAndRecipientUserIdShouldRespectOwnership() {
                User recipient = userRepository
                                .save(UserTestBuilder.validUser().withEmail("recipient3@example.com").build());
                User otherRecipient = userRepository
                                .save(UserTestBuilder.validUser().withEmail("recipient4@example.com").build());
                User actor = userRepository.save(UserTestBuilder.validUser().withEmail("actor3@example.com").build());

                Notification notification = saveNotification(recipient, actor,
                                NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED,
                                Instant.parse("2026-04-04T08:00:00Z"), null);

                Optional<Notification> owned = notificationRepository.findByIdAndRecipientUserId(notification.getId(),
                                recipient.getId());
                Optional<Notification> foreign = notificationRepository.findByIdAndRecipientUserId(notification.getId(),
                                otherRecipient.getId());

                assertThat(owned).isPresent();
                assertThat(foreign).isEmpty();
        }

        @Test
        void existsByRecipientUserIdAndActorUserIdAndTypeAndProjectIdShouldMatchExactDuplicateNotification() {
                User recipient = userRepository
                                .save(UserTestBuilder.validUser().withEmail("recipient7@example.com").build());
                User actor = userRepository.save(UserTestBuilder.validUser().withEmail("actor7@example.com").build());
                User otherActor = userRepository
                                .save(UserTestBuilder.validUser().withEmail("actor8@example.com").build());

                Notification notification = new Notification();
                notification.setRecipientUser(recipient);
                notification.setActorUser(actor);
                notification.setType(NotificationType.PROJECT_ANNOTATION_COMPLETED);
                notification.setProjectId(321L);
                notification.setProjectName("NER Project");
                notification.setResearchGroupId(99L);
                notification.setResearchGroupName("Annotations Group");
                notificationRepository.save(notification);

                boolean exists = notificationRepository.existsByRecipientUserIdAndActorUserIdAndTypeAndProjectId(
                                recipient.getId(),
                                actor.getId(),
                                NotificationType.PROJECT_ANNOTATION_COMPLETED,
                                321L);

                boolean differentActor = notificationRepository
                                .existsByRecipientUserIdAndActorUserIdAndTypeAndProjectId(
                                                recipient.getId(),
                                                otherActor.getId(),
                                                NotificationType.PROJECT_ANNOTATION_COMPLETED,
                                                321L);

                boolean differentType = notificationRepository.existsByRecipientUserIdAndActorUserIdAndTypeAndProjectId(
                                recipient.getId(),
                                actor.getId(),
                                NotificationType.PROJECT_PARTICIPANT_ASSIGNED,
                                321L);

                boolean differentProject = notificationRepository
                                .existsByRecipientUserIdAndActorUserIdAndTypeAndProjectId(
                                                recipient.getId(),
                                                actor.getId(),
                                                NotificationType.PROJECT_ANNOTATION_COMPLETED,
                                                999L);

                assertThat(exists).isTrue();
                assertThat(differentActor).isFalse();
                assertThat(differentType).isFalse();
                assertThat(differentProject).isFalse();
        }

        @Test
        void markAllAsReadByRecipientUserIdShouldOnlyUpdateUnreadForRecipient() {
                User recipient = userRepository
                                .save(UserTestBuilder.validUser().withEmail("recipient5@example.com").build());
                User otherRecipient = userRepository
                                .save(UserTestBuilder.validUser().withEmail("recipient6@example.com").build());
                User actor = userRepository.save(UserTestBuilder.validUser().withEmail("actor4@example.com").build());

                Notification unreadMine = saveNotification(recipient, actor,
                                NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED,
                                Instant.parse("2026-04-04T08:00:00Z"), null);
                Notification readMine = saveNotification(recipient, actor,
                                NotificationType.RESEARCH_GROUP_INVITATION_ACCEPTED,
                                Instant.parse("2026-04-04T09:00:00Z"), Instant.parse("2026-04-04T09:15:00Z"));
                Notification unreadOther = saveNotification(otherRecipient, actor,
                                NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED,
                                Instant.parse("2026-04-04T10:00:00Z"), null);

                Instant markTime = Instant.parse("2026-04-04T12:00:00Z");
                int updated = notificationRepository.markAllAsReadByRecipientUserId(recipient.getId(), markTime);

                entityManager.flush();
                entityManager.clear();

                Notification refreshedUnreadMine = notificationRepository.findById(unreadMine.getId()).orElseThrow();
                Notification refreshedReadMine = notificationRepository.findById(readMine.getId()).orElseThrow();
                Notification refreshedUnreadOther = notificationRepository.findById(unreadOther.getId()).orElseThrow();

                assertThat(updated).isEqualTo(1);
                assertThat(refreshedUnreadMine.getReadAt()).isEqualTo(markTime);
                assertThat(refreshedReadMine.getReadAt()).isEqualTo(Instant.parse("2026-04-04T09:15:00Z"));
                assertThat(refreshedUnreadOther.getReadAt()).isNull();
        }

        private Notification saveNotification(
                        User recipient,
                        User actor,
                        NotificationType type,
                        Instant createdAt,
                        Instant readAt) {
                Notification notification = new Notification();
                notification.setRecipientUser(recipient);
                notification.setActorUser(actor);
                notification.setType(type);
                notification.setResearchGroupId(77L);
                notification.setResearchGroupName("Test Group");
                notification.setReadAt(readAt);

                Notification saved = notificationRepository.save(notification);
                setField(saved, "createdAt", createdAt);
                return notificationRepository.save(saved);
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
