package es.udc.fic.corpuslab.modules.project.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
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
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.notification.entities.Notification;
import es.udc.fic.corpuslab.modules.notification.enums.NotificationType;
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;
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
class ProjectAnnotationIntegrationTest extends AbstractIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

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
        private PasswordEncoder passwordEncoder;

        private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

        @BeforeEach
        void cleanData() {
                notificationRepository.deleteAll();
                invitationRepository.deleteAll();
                memberRepository.deleteAll();
                datasetItemRepository.deleteAll();
                projectParticipantRepository.deleteAll();
                projectRepository.deleteAll();
                researchGroupRepository.deleteAll();
                userRepository.deleteAll();
        }

        @Test
        void shouldSaveAnnotationStepAndReflectParticipantProgress() throws Exception {
                User owner = createUser("owner.annotation.progress@example.com");
                User annotator = createUser("annotator.annotation.progress@example.com");

                ResearchGroup group = createGroup("Annotation Progress Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "CSV Annotation Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                DatasetItem item = createCsvDatasetItem(project, "id,text\n1,alpha\n2,beta\n3,gamma");

                String token = loginAs("annotator.annotation.progress@example.com");

                mockMvc.perform(get("/api/projects/{projectId}/annotations/steps", project.getId())
                                .param("offset", "0")
                                .param("limit", "10")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.projectId").value(project.getId()))
                                .andExpect(jsonPath("$.totalSteps").value(3))
                                .andExpect(jsonPath("$.completedSteps").value(0))
                                .andExpect(jsonPath("$.completionPercentage").value(0))
                                .andExpect(jsonPath("$.steps.length()").value(3))
                                .andExpect(jsonPath("$.steps[0].preview").value("id,text\n1,alpha"))
                                .andExpect(jsonPath("$.steps[1].preview").value("id,text\n2,beta"))
                                .andExpect(jsonPath("$.steps[2].preview").value("id,text\n3,gamma"));

                mockMvc.perform(put("/api/projects/{projectId}/annotations/steps", project.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "datasetItemId", item.getId(),
                                                "stepIndex", 0,
                                                "annotation", Map.of("label", "POSITIVE")))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.datasetItemId").value(item.getId()))
                                .andExpect(jsonPath("$.stepIndex").value(0))
                                .andExpect(jsonPath("$.participantCompletedSteps").value(1))
                                .andExpect(jsonPath("$.participantTotalSteps").value(3))
                                .andExpect(jsonPath("$.participantCompletionPercentage").value(33));

                mockMvc.perform(get("/api/projects/{projectId}", project.getId())
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.participants[?(@.userId==" + annotator.getId()
                                                + ")].completionPercentage")
                                                .value(hasItem(33)));

                assertThat(notificationRepository.findAll()).isEmpty();
        }

        @Test
        void shouldNotifyOwnerWhenAnnotatorCompletesAllSteps() throws Exception {
                User owner = createUser("owner.annotation.complete@example.com");
                User annotator = createUser("annotator.annotation.complete@example.com");

                ResearchGroup group = createGroup("Annotation Completion Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Completion Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                DatasetItem item = createCsvDatasetItem(project, "id,text\n1,alpha\n2,beta");

                String token = loginAs("annotator.annotation.complete@example.com");

                saveAnnotationStep(token, project.getId(), item.getId(), 0, "POSITIVE");
                saveAnnotationStep(token, project.getId(), item.getId(), 1, "NEGATIVE");
                saveAnnotationStep(token, project.getId(), item.getId(), 1, "NEGATIVE");

                Long ownerId = owner.getId();
                Long annotatorId = annotator.getId();
                Long projectId = project.getId();
                String projectName = project.getName();

                assertThat(notificationRepository.findAll())
                                .hasSize(1)
                                .allSatisfy(notification -> {
                                        assertThat(notification.getType())
                                                        .isEqualTo(NotificationType.PROJECT_ANNOTATION_COMPLETED);
                                        assertThat(notification.getRecipientUser().getId()).isEqualTo(ownerId);
                                        assertThat(notification.getActorUser().getId()).isEqualTo(annotatorId);
                                        assertThat(notification.getProjectId()).isEqualTo(projectId);
                                });

                Notification notification = notificationRepository.findAll().getFirst();
                assertThat(notification.getProjectName()).isEqualTo(projectName);
        }

        @Test
        void shouldReturnDatasetItemBinaryContentForAnnotationViewer() throws Exception {
                User owner = createUser("owner.annotation.source@example.com");
                User annotator = createUser("annotator.annotation.source@example.com");

                ResearchGroup group = createGroup("Annotation Source Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Source Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                byte[] sourceBytes = "pdf-content".getBytes(StandardCharsets.UTF_8);

                DatasetItem item = new DatasetItem();
                item.setProject(project);
                item.setItemIndex(0);
                item.setContent(Map.of(
                                "fileName", "source.pdf",
                                "mimeType", "application/pdf",
                                "sizeBytes", sourceBytes.length,
                                "base64", Base64.getEncoder().encodeToString(sourceBytes)));
                item = datasetItemRepository.save(item);

                String token = loginAs("annotator.annotation.source@example.com");

                mockMvc.perform(
                                get("/api/projects/{projectId}/dataset-items/{datasetItemId}/content", project.getId(),
                                                item.getId())
                                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type", "application/pdf"))
                                .andExpect(content().bytes(sourceBytes));
        }

        @Test
        void shouldParseCsvWorkspaceWhenBase64ContainsLineBreaks() throws Exception {
                User owner = createUser("owner.annotation.csv.mime@example.com");
                User annotator = createUser("annotator.annotation.csv.mime@example.com");

                ResearchGroup group = createGroup("Annotation CSV MIME Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "CSV MIME Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                String longValue = "a".repeat(140);
                DatasetItem item = createCsvDatasetItemWithMimeBase64(
                                project,
                                "id,text\n1," + longValue + "\n2,beta");

                String token = loginAs("annotator.annotation.csv.mime@example.com");

                mockMvc.perform(get("/api/projects/{projectId}/annotations/steps", project.getId())
                                .param("offset", "0")
                                .param("limit", "10")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.projectId").value(project.getId()))
                                .andExpect(jsonPath("$.totalSteps").value(2))
                                .andExpect(jsonPath("$.steps.length()").value(2))
                                .andExpect(jsonPath("$.steps[0].datasetItemId").value(item.getId()));
        }

        @Test
        void shouldReturnDatasetItemBinaryContentWhenBase64StoredAsDataUri() throws Exception {
                User owner = createUser("owner.annotation.source.datauri@example.com");
                User annotator = createUser("annotator.annotation.source.datauri@example.com");

                ResearchGroup group = createGroup("Annotation Source Data URI Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Source Data URI Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                byte[] sourceBytes = "source-with-data-uri".getBytes(StandardCharsets.UTF_8);

                DatasetItem item = new DatasetItem();
                item.setProject(project);
                item.setItemIndex(0);
                item.setContent(Map.of(
                                "fileName", "source.txt",
                                "mimeType", "text/plain",
                                "sizeBytes", sourceBytes.length,
                                "base64", "data:text/plain;base64,"
                                                + Base64.getEncoder().encodeToString(sourceBytes)));
                item = datasetItemRepository.save(item);

                String token = loginAs("annotator.annotation.source.datauri@example.com");

                mockMvc.perform(
                                get("/api/projects/{projectId}/dataset-items/{datasetItemId}/content", project.getId(),
                                                item.getId())
                                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type", "text/plain"))
                                .andExpect(content().bytes(sourceBytes));
        }

        @Test
        void shouldSaveNerAnnotationWithEntitiesAndOffsets() throws Exception {
                User owner = createUser("owner.annotation.ner@example.com");
                User annotator = createUser("annotator.annotation.ner@example.com");

                ResearchGroup group = createGroup("Annotation NER Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "NER Annotation Project");
                project.setProjectType(ProjectType.NER);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                DatasetItem item = createTextDatasetItem(project, "John works at OpenAI");

                String token = loginAs("annotator.annotation.ner@example.com");

                mockMvc.perform(put("/api/projects/{projectId}/annotations/steps", project.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "datasetItemId", item.getId(),
                                                "stepIndex", 0,
                                                "annotation", Map.of(
                                                                "entities", List.of(
                                                                                Map.of(
                                                                                                "label", "PERSON",
                                                                                                "text", "John",
                                                                                                "startOffset", 0,
                                                                                                "endOffset", 4),
                                                                                Map.of(
                                                                                                "label", "ORG",
                                                                                                "text", "OpenAI",
                                                                                                "startOffset", 14,
                                                                                                "endOffset", 20)))))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.datasetItemId").value(item.getId()))
                                .andExpect(jsonPath("$.participantCompletedSteps").value(1));

                mockMvc.perform(get("/api/projects/{projectId}/annotations/steps", project.getId())
                                .param("offset", "0")
                                .param("limit", "10")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.steps[0].annotation.entities.length()").value(2))
                                .andExpect(jsonPath("$.steps[0].annotation.entities[0].label").value("PERSON"))
                                .andExpect(jsonPath("$.steps[0].annotation.entities[1].label").value("ORG"));
        }

        @Test
        void shouldRejectNerAnnotationWhenOffsetsDoNotMatchSelectedTextLength() throws Exception {
                User owner = createUser("owner.annotation.ner.invalid@example.com");
                User annotator = createUser("annotator.annotation.ner.invalid@example.com");

                ResearchGroup group = createGroup("Annotation NER Invalid Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "NER Invalid Annotation Project");
                project.setProjectType(ProjectType.NER);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                DatasetItem item = createTextDatasetItem(project, "John works at OpenAI");

                String token = loginAs("annotator.annotation.ner.invalid@example.com");

                mockMvc.perform(put("/api/projects/{projectId}/annotations/steps", project.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "datasetItemId", item.getId(),
                                                "stepIndex", 0,
                                                "annotation", Map.of(
                                                                "entities", List.of(
                                                                                Map.of(
                                                                                                "label", "PERSON",
                                                                                                "text", "John",
                                                                                                "startOffset", 0,
                                                                                                "endOffset", 5)))))))
                                .andExpect(status().isBadRequest());
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
                return projectRepository.save(project);
        }

        private void assign(Project project, User user, ProjectParticipantRole role) {
                ProjectParticipant participant = new ProjectParticipant();
                participant.setProject(project);
                participant.setUser(user);
                participant.setRole(role);
                projectParticipantRepository.save(participant);
        }

        private DatasetItem createCsvDatasetItem(Project project, String csvContent) {
                byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);

                DatasetItem item = new DatasetItem();
                item.setProject(project);
                item.setItemIndex(0);
                item.setContent(Map.of(
                                "fileName", "dataset.csv",
                                "mimeType", "text/csv",
                                "sizeBytes", bytes.length,
                                "base64", Base64.getEncoder().encodeToString(bytes)));

                return datasetItemRepository.save(item);
        }

        private DatasetItem createCsvDatasetItemWithMimeBase64(Project project, String csvContent) {
                byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);

                DatasetItem item = new DatasetItem();
                item.setProject(project);
                item.setItemIndex(0);
                item.setContent(Map.of(
                                "fileName", "dataset-mime.csv",
                                "mimeType", "text/csv",
                                "sizeBytes", bytes.length,
                                "base64", Base64.getMimeEncoder().encodeToString(bytes)));

                return datasetItemRepository.save(item);
        }

        private DatasetItem createTextDatasetItem(Project project, String textContent) {
                byte[] bytes = textContent.getBytes(StandardCharsets.UTF_8);

                DatasetItem item = new DatasetItem();
                item.setProject(project);
                item.setItemIndex(0);
                item.setContent(Map.of(
                                "fileName", "dataset.txt",
                                "mimeType", "text/plain",
                                "sizeBytes", bytes.length,
                                "base64", Base64.getEncoder().encodeToString(bytes)));

                return datasetItemRepository.save(item);
        }

        private void saveAnnotationStep(
                        String token,
                        Long projectId,
                        Long datasetItemId,
                        int stepIndex,
                        String label) throws Exception {
                mockMvc.perform(put("/api/projects/{projectId}/annotations/steps", projectId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "datasetItemId", datasetItemId,
                                                "stepIndex", stepIndex,
                                                "annotation", Map.of("label", label)))))
                                .andExpect(status().isOk());
        }
}
