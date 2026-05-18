package es.udc.fic.corpuslab.modules.project.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
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
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
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
        void shouldMarkStepAsPendingWhenAnnotationIsCleared() throws Exception {
                User owner = createUser("owner.annotation.clear@example.com");
                User annotator = createUser("annotator.annotation.clear@example.com");

                ResearchGroup group = createGroup("Annotation Clear Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Annotation Clear Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                DatasetItem item = createCsvDatasetItem(project, "id,text\n1,alpha\n2,beta");

                String token = loginAs("annotator.annotation.clear@example.com");

                saveAnnotationStep(token, project.getId(), item.getId(), 0, "POSITIVE");

                Map<String, Object> clearRequest = new LinkedHashMap<>();
                clearRequest.put("datasetItemId", item.getId());
                clearRequest.put("stepIndex", 0);
                clearRequest.put("annotation", null);

                mockMvc.perform(put("/api/projects/{projectId}/annotations/steps", project.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(clearRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.participantCompletedSteps").value(0))
                                .andExpect(jsonPath("$.participantCompletionPercentage").value(0));

                mockMvc.perform(get("/api/projects/{projectId}/annotations/steps", project.getId())
                                .param("offset", "0")
                                .param("limit", "10")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.completedSteps").value(0))
                                .andExpect(jsonPath("$.completionPercentage").value(0))
                                .andExpect(jsonPath("$.firstPendingStepIndex").value(1))
                                .andExpect(jsonPath("$.steps[0].completed").value(false));
        }

        @Test
        void shouldReturnFirstPendingStepIndexForPartialAndCompletedProgress() throws Exception {
                User owner = createUser("owner.annotation.pending-index@example.com");
                User annotator = createUser("annotator.annotation.pending-index@example.com");

                ResearchGroup group = createGroup("Annotation Pending Index Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Annotation Pending Index Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                DatasetItem item = createCsvDatasetItem(project, "id,text\n1,a\n2,b\n3,c\n4,d\n5,e\n6,f");

                String token = loginAs("annotator.annotation.pending-index@example.com");

                saveAnnotationStep(token, project.getId(), item.getId(), 0, "POSITIVE");
                saveAnnotationStep(token, project.getId(), item.getId(), 1, "POSITIVE");
                saveAnnotationStep(token, project.getId(), item.getId(), 3, "POSITIVE");
                saveAnnotationStep(token, project.getId(), item.getId(), 4, "POSITIVE");
                saveAnnotationStep(token, project.getId(), item.getId(), 5, "POSITIVE");

                mockMvc.perform(get("/api/projects/{projectId}/annotations/steps", project.getId())
                                .param("offset", "0")
                                .param("limit", "10")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstPendingStepIndex").value(3));

                saveAnnotationStep(token, project.getId(), item.getId(), 2, "POSITIVE");

                mockMvc.perform(get("/api/projects/{projectId}/annotations/steps", project.getId())
                                .param("offset", "0")
                                .param("limit", "10")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.completionPercentage").value(100))
                                .andExpect(jsonPath("$.firstPendingStepIndex").value(1));
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
                                "stepCount", 1,
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
                                "stepCount", 1,
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

        @Test
        void shouldSanitizeAnnotationWorkspacePaginationParameters() throws Exception {
                User owner = createUser("owner.annotation.workspace.pagination@example.com");
                User annotator = createUser("annotator.annotation.workspace.pagination@example.com");

                ResearchGroup group = createGroup("Annotation Workspace Pagination Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Workspace Pagination Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                createCsvDatasetItem(project, "id,text\n1,alpha\n2,beta\n3,gamma");

                String token = loginAs("annotator.annotation.workspace.pagination@example.com");

                mockMvc.perform(get("/api/projects/{projectId}/annotations/steps", project.getId())
                                .param("offset", "-10")
                                .param("limit", "0")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.offset").value(0))
                                .andExpect(jsonPath("$.limit").value(50))
                                .andExpect(jsonPath("$.steps.length()").value(3));

                mockMvc.perform(get("/api/projects/{projectId}/annotations/steps", project.getId())
                                .param("offset", "1")
                                .param("limit", "999")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.offset").value(1))
                                .andExpect(jsonPath("$.limit").value(250))
                                .andExpect(jsonPath("$.steps.length()").value(2));
        }

        @Test
        void shouldReturnNotFoundWhenReadingAnnotationWorkspaceWithoutProjectAssignment() throws Exception {
                User owner = createUser("owner.annotation.workspace.access@example.com");
                User outsider = createUser("outsider.annotation.workspace.access@example.com");

                ResearchGroup group = createGroup("Annotation Workspace Access Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);

                Project project = createProject(group, "Workspace Access Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                createCsvDatasetItem(project, "id,text\n1,alpha");

                String token = loginAs("outsider.annotation.workspace.access@example.com");

                mockMvc.perform(get("/api/projects/{projectId}/annotations/steps", project.getId())
                                .param("offset", "0")
                                .param("limit", "10")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldAllowProjectCreatorToViewInvestigatorAnnotations() throws Exception {
                User owner = createUser("owner.annotation.review@example.com");
                User annotator = createUser("annotator.annotation.review@example.com");

                ResearchGroup group = createGroup("Annotation Review Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Annotation Review Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                DatasetItem item = createCsvDatasetItem(project, "id,text\n1,alpha\n2,beta");

                String annotatorToken = loginAs("annotator.annotation.review@example.com");
                saveAnnotationStep(annotatorToken, project.getId(), item.getId(), 0, "POSITIVE");

                String ownerToken = loginAs("owner.annotation.review@example.com");

                mockMvc.perform(
                                get("/api/projects/{projectId}/annotations/participants/{participantUserId}/steps",
                                                project.getId(), annotator.getId())
                                                .param("offset", "0")
                                                .param("limit", "10")
                                                .header("Authorization", "Bearer " + ownerToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.projectId").value(project.getId()))
                                .andExpect(jsonPath("$.totalSteps").value(2))
                                .andExpect(jsonPath("$.completedSteps").value(1))
                                .andExpect(jsonPath("$.completionPercentage").value(50))
                                .andExpect(jsonPath("$.steps.length()").value(2))
                                .andExpect(jsonPath("$.steps[0].completed").value(true))
                                .andExpect(jsonPath("$.steps[0].annotation.label").value("POSITIVE"))
                                .andExpect(jsonPath("$.steps[1].completed").value(false));
        }

        @Test
        void shouldReturnForbiddenWhenRequesterIsNotProjectCreatorForAnnotationReview() throws Exception {
                User owner = createUser("owner.annotation.review.forbidden@example.com");
                User annotatorA = createUser("annotatora.annotation.review.forbidden@example.com");
                User annotatorB = createUser("annotatorb.annotation.review.forbidden@example.com");

                ResearchGroup group = createGroup("Annotation Review Forbidden Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotatorA, group, ResearchGroupMemberRole.ANNOTATOR);
                addMembership(annotatorB, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Annotation Review Forbidden Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotatorA, ProjectParticipantRole.PARTICIPANT);
                assign(project, annotatorB, ProjectParticipantRole.PARTICIPANT);

                String annotatorToken = loginAs("annotatora.annotation.review.forbidden@example.com");

                mockMvc.perform(
                                get("/api/projects/{projectId}/annotations/participants/{participantUserId}/steps",
                                                project.getId(), annotatorB.getId())
                                                .param("offset", "0")
                                                .param("limit", "10")
                                                .header("Authorization", "Bearer " + annotatorToken))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnBadRequestWhenCreatorRequestsAnnotationsForNonInvestigator() throws Exception {
                User owner = createUser("owner.annotation.review.badrequest@example.com");
                User annotator = createUser("annotator.annotation.review.badrequest@example.com");

                ResearchGroup group = createGroup("Annotation Review Bad Request Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Annotation Review Bad Request Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                String ownerToken = loginAs("owner.annotation.review.badrequest@example.com");

                mockMvc.perform(
                                get("/api/projects/{projectId}/annotations/participants/{participantUserId}/steps",
                                                project.getId(), owner.getId())
                                                .param("offset", "0")
                                                .param("limit", "10")
                                                .header("Authorization", "Bearer " + ownerToken))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnNotFoundWhenRequesterIsNotAssignedForAnnotationReview() throws Exception {
                User owner = createUser("owner.annotation.review.notfound@example.com");
                User annotator = createUser("annotator.annotation.review.notfound@example.com");
                User outsider = createUser("outsider.annotation.review.notfound@example.com");

                ResearchGroup group = createGroup("Annotation Review Not Found Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Annotation Review Not Found Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                String outsiderToken = loginAs("outsider.annotation.review.notfound@example.com");

                mockMvc.perform(
                                get("/api/projects/{projectId}/annotations/participants/{participantUserId}/steps",
                                                project.getId(), annotator.getId())
                                                .param("offset", "0")
                                                .param("limit", "10")
                                                .header("Authorization", "Bearer " + outsiderToken))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldSanitizePaginationParametersWhenCreatorReviewsInvestigatorAnnotations() throws Exception {
                User owner = createUser("owner.annotation.review.pagination@example.com");
                User annotator = createUser("annotator.annotation.review.pagination@example.com");

                ResearchGroup group = createGroup("Annotation Review Pagination Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Annotation Review Pagination Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                createCsvDatasetItem(project, "id,text\n1,alpha\n2,beta\n3,gamma");

                String ownerToken = loginAs("owner.annotation.review.pagination@example.com");

                mockMvc.perform(
                                get("/api/projects/{projectId}/annotations/participants/{participantUserId}/steps",
                                                project.getId(), annotator.getId())
                                                .param("offset", "-10")
                                                .param("limit", "0")
                                                .header("Authorization", "Bearer " + ownerToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.offset").value(0))
                                .andExpect(jsonPath("$.limit").value(50))
                                .andExpect(jsonPath("$.steps.length()").value(3));

                mockMvc.perform(
                                get("/api/projects/{projectId}/annotations/participants/{participantUserId}/steps",
                                                project.getId(), annotator.getId())
                                                .param("offset", "1")
                                                .param("limit", "999")
                                                .header("Authorization", "Bearer " + ownerToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.offset").value(1))
                                .andExpect(jsonPath("$.limit").value(250))
                                .andExpect(jsonPath("$.steps.length()").value(2));
        }

        @Test
        void shouldReturnReviewedInvestigatorAnnotationsInsteadOfCreatorOnes() throws Exception {
                User owner = createUser("owner.annotation.review.target.progress@example.com");
                User annotator = createUser("annotator.annotation.review.target.progress@example.com");

                ResearchGroup group = createGroup("Annotation Review Target Progress Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Annotation Review Target Progress Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                DatasetItem item = createCsvDatasetItem(project, "id,text\n1,alpha\n2,beta");

                String ownerToken = loginAs("owner.annotation.review.target.progress@example.com");
                String annotatorToken = loginAs("annotator.annotation.review.target.progress@example.com");

                saveAnnotationStep(ownerToken, project.getId(), item.getId(), 0, "OWNER_ONLY");
                saveAnnotationStep(annotatorToken, project.getId(), item.getId(), 0, "PARTICIPANT_ONLY");
                saveAnnotationStep(annotatorToken, project.getId(), item.getId(), 1, "NEGATIVE");

                mockMvc.perform(
                                get("/api/projects/{projectId}/annotations/participants/{participantUserId}/steps",
                                                project.getId(), annotator.getId())
                                                .param("offset", "0")
                                                .param("limit", "10")
                                                .header("Authorization", "Bearer " + ownerToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.completedSteps").value(2))
                                .andExpect(jsonPath("$.completionPercentage").value(100))
                                .andExpect(jsonPath("$.steps[0].annotation.label").value("PARTICIPANT_ONLY"))
                                .andExpect(jsonPath("$.steps[1].annotation.label").value("NEGATIVE"));
        }

        @Test
        void shouldReturnBadRequestWhenCreatorReviewsUserNotAssignedToProject() throws Exception {
                User owner = createUser("owner.annotation.review.target.unassigned@example.com");
                User annotator = createUser("annotator.annotation.review.target.unassigned@example.com");
                User unassignedUser = createUser("unassigned.annotation.review.target@example.com");

                ResearchGroup group = createGroup("Annotation Review Unassigned Target Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Annotation Review Unassigned Target Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                String ownerToken = loginAs("owner.annotation.review.target.unassigned@example.com");

                mockMvc.perform(
                                get("/api/projects/{projectId}/annotations/participants/{participantUserId}/steps",
                                                project.getId(), unassignedUser.getId())
                                                .param("offset", "0")
                                                .param("limit", "10")
                                                .header("Authorization", "Bearer " + ownerToken))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldAllowProjectCreatorToReadDatasetItemSourceContentForAnnotationReview() throws Exception {
                User owner = createUser("owner.annotation.review.source@example.com");
                User annotator = createUser("annotator.annotation.review.source@example.com");

                ResearchGroup group = createGroup("Annotation Review Source Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Annotation Review Source Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                byte[] sourceBytes = "review-source-content".getBytes(StandardCharsets.UTF_8);
                DatasetItem item = createBinaryDatasetItem(
                                project,
                                "review-source.txt",
                                "text/plain",
                                Base64.getEncoder().encodeToString(sourceBytes));

                String ownerToken = loginAs("owner.annotation.review.source@example.com");

                mockMvc.perform(
                                get("/api/projects/{projectId}/dataset-items/{datasetItemId}/content", project.getId(),
                                                item.getId())
                                                .header("Authorization", "Bearer " + ownerToken))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type", "text/plain"))
                                .andExpect(content().bytes(sourceBytes));
        }

        @Test
        void shouldReturnBadRequestWhenSavingAnnotationWithInvalidStepIndex() throws Exception {
                User owner = createUser("owner.annotation.step.invalid.index@example.com");
                User annotator = createUser("annotator.annotation.step.invalid.index@example.com");

                ResearchGroup group = createGroup("Annotation Invalid Step Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Invalid Step Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                DatasetItem item = createCsvDatasetItem(project, "id,text\n1,alpha\n2,beta");
                String token = loginAs("annotator.annotation.step.invalid.index@example.com");

                mockMvc.perform(put("/api/projects/{projectId}/annotations/steps", project.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "datasetItemId", item.getId(),
                                                "stepIndex", 2,
                                                "annotation", Map.of("label", "POSITIVE")))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnBadRequestWhenSavingAnnotationWithEmptyPayload() throws Exception {
                User owner = createUser("owner.annotation.payload.empty@example.com");
                User annotator = createUser("annotator.annotation.payload.empty@example.com");

                ResearchGroup group = createGroup("Annotation Empty Payload Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Empty Payload Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                DatasetItem item = createCsvDatasetItem(project, "id,text\n1,alpha");
                String token = loginAs("annotator.annotation.payload.empty@example.com");

                mockMvc.perform(put("/api/projects/{projectId}/annotations/steps", project.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "datasetItemId", item.getId(),
                                                "stepIndex", 0,
                                                "annotation", Map.of()))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnBadRequestWhenDatasetItemBelongsToAnotherProject() throws Exception {
                User owner = createUser("owner.annotation.dataset.mismatch@example.com");
                User annotator = createUser("annotator.annotation.dataset.mismatch@example.com");

                ResearchGroup group = createGroup("Annotation Dataset Mismatch Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project targetProject = createProject(group, "Target Project");
                targetProject.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                targetProject = projectRepository.save(targetProject);

                Project foreignProject = createProject(group, "Foreign Project");
                foreignProject.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                foreignProject = projectRepository.save(foreignProject);

                assign(targetProject, owner, ProjectParticipantRole.CREATOR);
                assign(targetProject, annotator, ProjectParticipantRole.PARTICIPANT);
                assign(foreignProject, owner, ProjectParticipantRole.CREATOR);

                DatasetItem foreignItem = createCsvDatasetItem(foreignProject, "id,text\n1,foreign");
                String token = loginAs("annotator.annotation.dataset.mismatch@example.com");

                mockMvc.perform(put("/api/projects/{projectId}/annotations/steps", targetProject.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "datasetItemId", foreignItem.getId(),
                                                "stepIndex", 0,
                                                "annotation", Map.of("label", "POSITIVE")))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldFallbackToOctetStreamWhenStoredMimeTypeIsInvalid() throws Exception {
                User owner = createUser("owner.annotation.mime.invalid@example.com");
                User annotator = createUser("annotator.annotation.mime.invalid@example.com");

                ResearchGroup group = createGroup("Annotation Invalid MIME Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Invalid MIME Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                byte[] sourceBytes = "invalid-mime-content".getBytes(StandardCharsets.UTF_8);
                DatasetItem item = createBinaryDatasetItem(
                                project,
                                "source.bin",
                                "invalid mime type",
                                Base64.getEncoder().encodeToString(sourceBytes));

                String token = loginAs("annotator.annotation.mime.invalid@example.com");

                mockMvc.perform(
                                get("/api/projects/{projectId}/dataset-items/{datasetItemId}/content", project.getId(),
                                                item.getId())
                                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type", "application/octet-stream"))
                                .andExpect(content().bytes(sourceBytes));
        }

        @Test
        void shouldDefaultToOctetStreamWhenStoredMimeTypeIsBlank() throws Exception {
                User owner = createUser("owner.annotation.mime.blank@example.com");
                User annotator = createUser("annotator.annotation.mime.blank@example.com");

                ResearchGroup group = createGroup("Annotation Blank MIME Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Blank MIME Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                byte[] sourceBytes = "blank-mime-content".getBytes(StandardCharsets.UTF_8);
                DatasetItem item = createBinaryDatasetItem(
                                project,
                                "source-no-mime.bin",
                                "   ",
                                Base64.getEncoder().encodeToString(sourceBytes));

                String token = loginAs("annotator.annotation.mime.blank@example.com");

                mockMvc.perform(
                                get("/api/projects/{projectId}/dataset-items/{datasetItemId}/content", project.getId(),
                                                item.getId())
                                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type", "application/octet-stream"))
                                .andExpect(content().bytes(sourceBytes));
        }

        @Test
        void shouldReturnBadRequestWhenDatasetItemSourceContentHasInvalidBase64() throws Exception {
                User owner = createUser("owner.annotation.base64.invalid@example.com");
                User annotator = createUser("annotator.annotation.base64.invalid@example.com");

                ResearchGroup group = createGroup("Annotation Invalid Base64 Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Invalid Base64 Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                DatasetItem item = createBinaryDatasetItem(project, "source-bad-base64.txt", "text/plain", "A");
                String token = loginAs("annotator.annotation.base64.invalid@example.com");

                mockMvc.perform(
                                get("/api/projects/{projectId}/dataset-items/{datasetItemId}/content", project.getId(),
                                                item.getId())
                                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnBadRequestWhenDatasetItemSourceContentIsBlank() throws Exception {
                User owner = createUser("owner.annotation.base64.blank@example.com");
                User annotator = createUser("annotator.annotation.base64.blank@example.com");

                ResearchGroup group = createGroup("Annotation Blank Base64 Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Blank Base64 Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                DatasetItem item = createBinaryDatasetItem(project, "source-blank-base64.txt", "text/plain", "   ");
                String token = loginAs("annotator.annotation.base64.blank@example.com");

                mockMvc.perform(
                                get("/api/projects/{projectId}/dataset-items/{datasetItemId}/content", project.getId(),
                                                item.getId())
                                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldNormalizeNerAnnotationOffsetsAndDeduplicateEntities() throws Exception {
                User owner = createUser("owner.annotation.ner.normalize@example.com");
                User annotator = createUser("annotator.annotation.ner.normalize@example.com");

                ResearchGroup group = createGroup("Annotation NER Normalize Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "NER Normalize Project");
                project.setProjectType(ProjectType.NER);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                DatasetItem item = createTextDatasetItem(project, "John works at OpenAI");

                String token = loginAs("annotator.annotation.ner.normalize@example.com");

                mockMvc.perform(put("/api/projects/{projectId}/annotations/steps", project.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "datasetItemId", item.getId(),
                                                "stepIndex", 0,
                                                "annotation", Map.of(
                                                                "entities", List.of(
                                                                                Map.of(
                                                                                                "label", " PERSON ",
                                                                                                "text", " John ",
                                                                                                "startOffset", "0",
                                                                                                "endOffset", "4"),
                                                                                Map.of(
                                                                                                "label", "PERSON",
                                                                                                "text", "John",
                                                                                                "startOffset", 0,
                                                                                                "endOffset", 4)),
                                                                "notes", "  keep note  ")))))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/projects/{projectId}/annotations/steps", project.getId())
                                .param("offset", "0")
                                .param("limit", "10")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.steps[0].annotation.entities.length()").value(1))
                                .andExpect(jsonPath("$.steps[0].annotation.entities[0].label").value("PERSON"))
                                .andExpect(jsonPath("$.steps[0].annotation.entities[0].text").value("John"))
                                .andExpect(jsonPath("$.steps[0].annotation.entities[0].startOffset").value(0))
                                .andExpect(jsonPath("$.steps[0].annotation.entities[0].endOffset").value(4))
                                .andExpect(jsonPath("$.steps[0].annotation.notes").value("keep note"));
        }

        @Test
        void shouldRejectNerAnnotationWithoutEntities() throws Exception {
                User owner = createUser("owner.annotation.ner.entities.required@example.com");
                User annotator = createUser("annotator.annotation.ner.entities.required@example.com");

                ResearchGroup group = createGroup("Annotation NER Required Entities Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "NER Required Entities Project");
                project.setProjectType(ProjectType.NER);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                DatasetItem item = createTextDatasetItem(project, "John works at OpenAI");

                String token = loginAs("annotator.annotation.ner.entities.required@example.com");

                mockMvc.perform(put("/api/projects/{projectId}/annotations/steps", project.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "datasetItemId", item.getId(),
                                                "stepIndex", 0,
                                                "annotation", Map.of("entities", List.of())))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldExportAnnotationResultsCsvForProjectCreator() throws Exception {
                User owner = createUser("owner.annotation.export@example.com");
                User annotator = createUser("annotator.annotation.export@example.com");

                ResearchGroup group = createGroup("Annotation Export Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Annotation Export Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project.setAnnotationTargetColumn("distil_predictions");
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);

                DatasetItem item = createCsvDatasetItem(
                                project,
                                "instance_id,text,distil_predictions\n1,alpha,{\"label\":\"a\"}\n2,beta,{\"label\":\"b\"}");

                String annotatorToken = loginAs("annotator.annotation.export@example.com");
                mockMvc.perform(put("/api/projects/{projectId}/annotations/steps", project.getId())
                                .header("Authorization", "Bearer " + annotatorToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "datasetItemId", item.getId(),
                                                "stepIndex", 0,
                                                "annotation", Map.of(
                                                                "label", "correcta",
                                                                "notes", "sin incidencias")))))
                                .andExpect(status().isOk());

                mockMvc.perform(put("/api/projects/{projectId}/annotations/steps", project.getId())
                                .header("Authorization", "Bearer " + annotatorToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                                "datasetItemId", item.getId(),
                                                "stepIndex", 1,
                                                "annotation", Map.of(
                                                                "label", "incorrecta",
                                                                "notes", "insult|borderline")))))
                                .andExpect(status().isOk());

                String ownerToken = loginAs("owner.annotation.export@example.com");

                MvcResult exportRequest = mockMvc
                                .perform(get("/api/projects/{projectId}/annotations/export", project.getId())
                                                .header("Authorization", "Bearer " + ownerToken))
                                .andExpect(request().asyncStarted())
                                .andReturn();

                MvcResult exportResult = mockMvc
                                .perform(asyncDispatch(exportRequest))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                                .andReturn();

                String csvContent = exportResult.getResponse().getContentAsString(StandardCharsets.UTF_8);

                assertThat(csvContent).contains("instance_id");
                assertThat(csvContent).contains("distil_predictions");
            assertThat(csvContent).contains(annotator.getId() + "_annotation");
            assertThat(csvContent).contains(annotator.getId() + "_coment");
            assertThat(csvContent).doesNotContain(annotator.getEmail().toLowerCase() + "_annotation");
            assertThat(csvContent).doesNotContain(annotator.getEmail().toLowerCase() + "_coment");
                assertThat(csvContent).contains("correcta");
                assertThat(csvContent).contains("insult|borderline");
                assertThat(csvContent).doesNotContain(annotator.getId() + "_binary_annotation");
                assertThat(csvContent).doesNotContain(annotator.getId() + "_error_typology");
        }

        @Test
        void shouldReturnForbiddenWhenParticipantExportsAnnotationResultsCsv() throws Exception {
                User owner = createUser("owner.annotation.export.forbidden@example.com");
                User annotator = createUser("annotator.annotation.export.forbidden@example.com");

                ResearchGroup group = createGroup("Annotation Export Forbidden Group");
                addMembership(owner, group, ResearchGroupMemberRole.OWNER);
                addMembership(annotator, group, ResearchGroupMemberRole.ANNOTATOR);

                Project project = createProject(group, "Annotation Export Forbidden Project");
                project.setProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE);
                project = projectRepository.save(project);

                assign(project, owner, ProjectParticipantRole.CREATOR);
                assign(project, annotator, ProjectParticipantRole.PARTICIPANT);
                createCsvDatasetItem(project, "instance_id,text\n1,alpha");

                String annotatorToken = loginAs("annotator.annotation.export.forbidden@example.com");

                mockMvc.perform(get("/api/projects/{projectId}/annotations/export", project.getId())
                                .header("Authorization", "Bearer " + annotatorToken))
                                .andExpect(status().isForbidden());
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
                                "stepCount", Math.max(0, csvContent.split("\\R", -1).length - 1),
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
                                "stepCount", Math.max(0, csvContent.split("\\R", -1).length - 1),
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
                                "stepCount", 1,
                                "base64", Base64.getEncoder().encodeToString(bytes)));

                return datasetItemRepository.save(item);
        }

        private DatasetItem createBinaryDatasetItem(Project project, String fileName, String mimeType, String base64) {
                DatasetItem item = new DatasetItem();
                item.setProject(project);
                item.setItemIndex(0);
                item.setContent(Map.of(
                                "fileName", fileName,
                                "mimeType", mimeType,
                                "sizeBytes", base64.length(),
                                "stepCount", 1,
                                "base64", base64));

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
