package es.udc.fic.corpuslab.modules.notification.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import es.udc.fic.corpuslab.AbstractIntegrationTest;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserLoginRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.notification.entities.Notification;
import es.udc.fic.corpuslab.modules.notification.enums.NotificationType;
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @BeforeEach
    void cleanData() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldListAuthenticatedUserNotifications() throws Exception {
        User recipient = userRepository.save(UserTestBuilder.validUser()
                .withEmail("recipient@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build());

        User actor = userRepository.save(UserTestBuilder.validUser()
                .withEmail("actor@example.com")
                .withFirstName("Ada")
                .withLastName("Lovelace")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build());

        Notification first = new Notification();
        first.setRecipientUser(recipient);
        first.setActorUser(actor);
        first.setType(NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED);
        first.setResearchGroupId(100L);
        first.setResearchGroupName("NLP Group");
        first.setInvitationId(700L);
        notificationRepository.save(first);

        Notification second = new Notification();
        second.setRecipientUser(recipient);
        second.setActorUser(actor);
        second.setType(NotificationType.RESEARCH_GROUP_INVITATION_ACCEPTED);
        second.setResearchGroupId(101L);
        second.setResearchGroupName("Corpus Group");
        second.setReadAt(java.time.Instant.now());
        notificationRepository.save(second);

        MockHttpSession session = loginAs("recipient@example.com");

        mockMvc.perform(get("/api/notifications")
                .param("limit", "10")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.length()").value(2))
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.notifications[0].id").isNumber())
                .andExpect(jsonPath("$.notifications[0].type").isString())
                .andExpect(jsonPath("$.notifications[0].read").isBoolean())
                .andExpect(jsonPath("$.notifications[0].createdAt").exists())
                .andExpect(jsonPath("$.notifications[0].actorFullName").value("Ada Lovelace"));
    }

    @Test
    void shouldMarkNotificationAsRead() throws Exception {
        User recipient = userRepository.save(UserTestBuilder.validUser()
                .withEmail("mark.read@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build());

        Notification notification = new Notification();
        notification.setRecipientUser(recipient);
        notification.setType(NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED);
        notification = notificationRepository.save(notification);

        MockHttpSession session = loginAs("mark.read@example.com");

        mockMvc.perform(post("/api/notifications/" + notification.getId() + "/read")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notification.getId()))
                .andExpect(jsonPath("$.read").value(true));

        Notification refreshed = notificationRepository.findById(notification.getId()).orElseThrow();
        assertThat(refreshed.getReadAt()).isNotNull();
    }

    @Test
    void shouldReturnNotFoundWhenMarkingNotificationFromAnotherUser() throws Exception {
        User owner = userRepository.save(UserTestBuilder.validUser()
                .withEmail("owner@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build());

        User other = userRepository.save(UserTestBuilder.validUser()
                .withEmail("other@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build());

        Notification foreignNotification = new Notification();
        foreignNotification.setRecipientUser(other);
        foreignNotification.setType(NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED);
        foreignNotification = notificationRepository.save(foreignNotification);

        MockHttpSession session = loginAs("owner@example.com");

        mockMvc.perform(post("/api/notifications/" + foreignNotification.getId() + "/read")
                .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Notification not found: " + foreignNotification.getId()));

        assertThat(owner.getId()).isNotNull();
    }

    @Test
    void shouldMarkAllNotificationsAsReadForAuthenticatedUser() throws Exception {
        User recipient = userRepository.save(UserTestBuilder.validUser()
                .withEmail("mark.all@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build());
        User other = userRepository.save(UserTestBuilder.validUser()
                .withEmail("mark.all.other@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build());

        Notification mineOne = new Notification();
        mineOne.setRecipientUser(recipient);
        mineOne.setType(NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED);
        mineOne = notificationRepository.save(mineOne);

        Notification mineTwo = new Notification();
        mineTwo.setRecipientUser(recipient);
        mineTwo.setType(NotificationType.RESEARCH_GROUP_INVITATION_ACCEPTED);
        mineTwo = notificationRepository.save(mineTwo);

        Notification foreign = new Notification();
        foreign.setRecipientUser(other);
        foreign.setType(NotificationType.RESEARCH_GROUP_INVITATION_RECEIVED);
        foreign = notificationRepository.save(foreign);

        MockHttpSession session = loginAs("mark.all@example.com");

        mockMvc.perform(post("/api/notifications/read-all")
                .session(session))
                .andExpect(status().isNoContent());

        Notification refreshedMineOne = notificationRepository.findById(mineOne.getId()).orElseThrow();
        Notification refreshedMineTwo = notificationRepository.findById(mineTwo.getId()).orElseThrow();
        Notification refreshedForeign = notificationRepository.findById(foreign.getId()).orElseThrow();

        assertThat(refreshedMineOne.getReadAt()).isNotNull();
        assertThat(refreshedMineTwo.getReadAt()).isNotNull();
        assertThat(refreshedForeign.getReadAt()).isNull();
    }

    @Test
    void shouldReturnForbiddenWithoutSession() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession loginAs(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        UserLoginRequestTestBuilder.validRequest()
                                .withEmail(email)
                                .build())))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
