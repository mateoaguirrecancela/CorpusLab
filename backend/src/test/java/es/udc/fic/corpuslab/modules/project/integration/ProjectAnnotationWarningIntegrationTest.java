package es.udc.fic.corpuslab.modules.project.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import es.udc.fic.corpuslab.AbstractIntegrationTest;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserLoginRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.OAuthLoginCodeRepository;
import es.udc.fic.corpuslab.modules.auth.repositories.PasswordResetTokenRepository;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.notification.entities.Notification;
import es.udc.fic.corpuslab.modules.notification.enums.NotificationType;
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import es.udc.fic.corpuslab.modules.project.shared.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.shared.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupMemberTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupInvitationRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectAnnotationWarningIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuthLoginCodeRepository oauthLoginCodeRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private ResearchGroupRepository researchGroupRepository;

    @Autowired
    private ResearchGroupMemberRepository memberRepository;

    @Autowired
    private ResearchGroupInvitationRepository invitationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectParticipantRepository projectParticipantRepository;

    @Autowired
    private DatasetItemRepository datasetItemRepository;

    @Autowired
    private AnnotationRepository annotationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @BeforeEach
    void cleanData() {
        notificationRepository.deleteAll();
        oauthLoginCodeRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        invitationRepository.deleteAll();
        annotationRepository.deleteAll();
        datasetItemRepository.deleteAll();
        projectParticipantRepository.deleteAll();
        projectRepository.deleteAll();
        memberRepository.deleteAll();
        researchGroupRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldToggleAnnotationWarningWhenRequesterIsCreator() throws Exception {
        User creator = createUser("creator.warning.toggle@example.com");
        User participantUser = createUser("participant.warning.toggle@example.com");

        ResearchGroup group = createGroup("Warning Toggle Group");
        addMembership(creator, group, ResearchGroupMemberRole.OWNER);
        addMembership(participantUser, group, ResearchGroupMemberRole.ANNOTATOR);

        Project project = createProject(group, "Warning Toggle Project");
        assign(project, creator, ProjectParticipantRole.CREATOR);
        assign(project, participantUser, ProjectParticipantRole.PARTICIPANT);

        DatasetItem item = createDatasetItem(project, 0);
        Annotation annotation = createAnnotation(item, participantUser, 0);

        String creatorToken = loginAs("creator.warning.toggle@example.com");

        mockMvc.perform(put("/api/projects/{projectId}/annotations/participants/{participantUserId}/steps/warning",
                project.getId(), participantUser.getId())
                .header("Authorization", "Bearer " + creatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "datasetItemId", item.getId(),
                        "stepIndex", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(project.getId()))
                .andExpect(jsonPath("$.datasetItemId").value(item.getId()))
                .andExpect(jsonPath("$.stepIndex").value(0))
                .andExpect(jsonPath("$.warning").value(true));

        Annotation warned = annotationRepository
                .findByDatasetItemIdAndUserIdAndStepIndex(item.getId(), participantUser.getId(), 0)
                .orElseThrow();
        assertThat(warned.isWarning()).isTrue();
        assertThat(warned.getWarningMarkedByUser()).isNotNull();
        assertThat(warned.getWarningMarkedByUser().getId()).isEqualTo(creator.getId());

        mockMvc.perform(put("/api/projects/{projectId}/annotations/participants/{participantUserId}/steps/warning",
                project.getId(), participantUser.getId())
                .header("Authorization", "Bearer " + creatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "datasetItemId", item.getId(),
                        "stepIndex", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warning").value(false));

        Annotation cleared = annotationRepository
                .findByDatasetItemIdAndUserIdAndStepIndex(item.getId(), participantUser.getId(), 0)
                .orElseThrow();
        assertThat(cleared.isWarning()).isFalse();
        assertThat(cleared.getWarningMarkedByUser()).isNull();
        assertThat(cleared.getWarningMarkedAt()).isNull();

        assertThat(notificationRepository.findAll())
                .extracting(Notification::getType)
                .contains(NotificationType.ANNOTATION_WARNING_MARKED, NotificationType.ANNOTATION_WARNING_CLEARED);
    }

    @Test
    void shouldResolveOwnAnnotationWarningAndNotifyCreator() throws Exception {
        User creator = createUser("creator.warning.resolve@example.com");
        User participantUser = createUser("participant.warning.resolve@example.com");

        ResearchGroup group = createGroup("Warning Resolve Group");
        addMembership(creator, group, ResearchGroupMemberRole.OWNER);
        addMembership(participantUser, group, ResearchGroupMemberRole.ANNOTATOR);

        Project project = createProject(group, "Warning Resolve Project");
        assign(project, creator, ProjectParticipantRole.CREATOR);
        assign(project, participantUser, ProjectParticipantRole.PARTICIPANT);

        DatasetItem item = createDatasetItem(project, 0);
        createAnnotation(item, participantUser, 0);

        String creatorToken = loginAs("creator.warning.resolve@example.com");
        String participantToken = loginAs("participant.warning.resolve@example.com");

        mockMvc.perform(put("/api/projects/{projectId}/annotations/participants/{participantUserId}/steps/warning",
                project.getId(), participantUser.getId())
                .header("Authorization", "Bearer " + creatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "datasetItemId", item.getId(),
                        "stepIndex", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warning").value(true));

        mockMvc.perform(put("/api/projects/{projectId}/annotations/steps/warning-resolution", project.getId())
                .header("Authorization", "Bearer " + participantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "datasetItemId", item.getId(),
                        "stepIndex", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(project.getId()))
                .andExpect(jsonPath("$.datasetItemId").value(item.getId()))
                .andExpect(jsonPath("$.stepIndex").value(0))
                .andExpect(jsonPath("$.warning").value(false));

        Annotation resolved = annotationRepository
                .findByDatasetItemIdAndUserIdAndStepIndex(item.getId(), participantUser.getId(), 0)
                .orElseThrow();
        assertThat(resolved.isWarning()).isFalse();
        assertThat(resolved.getWarningMarkedByUser()).isNull();

        assertThat(notificationRepository.findAll().stream()
                .anyMatch(notification -> notification.getType() == NotificationType.ANNOTATION_WARNING_RESOLVED
                        && notification.getRecipientUser().getId().equals(creator.getId())))
                .isTrue();
    }

    @Test
    void shouldReturnForbiddenWhenParticipantTogglesAnnotationWarning() throws Exception {
        User creator = createUser("creator.warning.forbidden@example.com");
        User participantUser = createUser("participant.warning.forbidden@example.com");

        ResearchGroup group = createGroup("Warning Forbidden Group");
        addMembership(creator, group, ResearchGroupMemberRole.OWNER);
        addMembership(participantUser, group, ResearchGroupMemberRole.ANNOTATOR);

        Project project = createProject(group, "Warning Forbidden Project");
        assign(project, creator, ProjectParticipantRole.CREATOR);
        assign(project, participantUser, ProjectParticipantRole.PARTICIPANT);

        DatasetItem item = createDatasetItem(project, 0);
        createAnnotation(item, participantUser, 0);

        String participantToken = loginAs("participant.warning.forbidden@example.com");

        mockMvc.perform(put("/api/projects/{projectId}/annotations/participants/{participantUserId}/steps/warning",
                project.getId(), participantUser.getId())
                .header("Authorization", "Bearer " + participantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "datasetItemId", item.getId(),
                        "stepIndex", 0))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnBadRequestWhenResolvingWarningThatIsNotActive() throws Exception {
        User creator = createUser("creator.warning.inactive@example.com");
        User participantUser = createUser("participant.warning.inactive@example.com");

        ResearchGroup group = createGroup("Warning Inactive Group");
        addMembership(creator, group, ResearchGroupMemberRole.OWNER);
        addMembership(participantUser, group, ResearchGroupMemberRole.ANNOTATOR);

        Project project = createProject(group, "Warning Inactive Project");
        assign(project, creator, ProjectParticipantRole.CREATOR);
        assign(project, participantUser, ProjectParticipantRole.PARTICIPANT);

        DatasetItem item = createDatasetItem(project, 0);
        createAnnotation(item, participantUser, 0);

        String participantToken = loginAs("participant.warning.inactive@example.com");

        mockMvc.perform(put("/api/projects/{projectId}/annotations/steps/warning-resolution", project.getId())
                .header("Authorization", "Bearer " + participantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "datasetItemId", item.getId(),
                        "stepIndex", 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnauthorizedWhenTogglingAnnotationWarningWithoutSession() throws Exception {
        mockMvc.perform(put("/api/projects/{projectId}/annotations/participants/{participantUserId}/steps/warning",
                1L, 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "datasetItemId", 1L,
                        "stepIndex", 0))))
                .andExpect(status().isUnauthorized());
    }

    private User createUser(String email) {
        User user = UserTestBuilder.validUser()
                .withEmail(email)
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        return userRepository.save(user);
    }

    private String loginAs(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        UserLoginRequestTestBuilder.validRequest().withEmail(email).build())))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private ResearchGroup createGroup(String name) {
        return researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().withName(name).build());
    }

    private void addMembership(User user, ResearchGroup group, ResearchGroupMemberRole role) {
        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(user)
                .withResearchGroup(group)
                .withRole(role)
                .build());
    }

    private Project createProject(ResearchGroup group, String name) {
        Project project = new Project();
        project.setResearchGroup(group);
        project.setName(name);
        project.setSetupCompleted(true);
        return projectRepository.save(project);
    }

    private void assign(Project project, User user, ProjectParticipantRole role) {
        ProjectParticipant participant = new ProjectParticipant();
        participant.setProject(project);
        participant.setUser(user);
        participant.setRole(role);
        projectParticipantRepository.save(participant);
    }

    private DatasetItem createDatasetItem(Project project, int itemIndex) {
        DatasetItem item = new DatasetItem();
        item.setProject(project);
        item.setItemIndex(itemIndex);
        item.setContent(Map.of(
                "fileName", "dataset.csv",
                "stepCount", 1));
        return datasetItemRepository.save(item);
    }

    private Annotation createAnnotation(DatasetItem datasetItem, User user, int stepIndex) {
        Annotation annotation = new Annotation();
        annotation.setDatasetItem(datasetItem);
        annotation.setUser(user);
        annotation.setStepIndex(stepIndex);
        annotation.setPayload(Map.of("label", "POSITIVE"));
        return annotationRepository.save(annotation);
    }
}
