package es.udc.fic.corpuslab.modules.project.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

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

import es.udc.fic.corpuslab.AbstractIntegrationTest;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserLoginRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupRequestDto;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.Project;
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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectSetupIntegrationTest extends AbstractIntegrationTest {

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
        private DatasetItemRepository datasetItemRepository;

        @Autowired
        private ProjectParticipantRepository projectParticipantRepository;

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

        private void createUser(String email) {
                User user = UserTestBuilder.validUser()
                                .withEmail(email)
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(user);
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

        private void createDatasetItem(Project project, String fileName, String mimeType, String content) {
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

                DatasetItem item = new DatasetItem();
                item.setProject(project);
                item.setItemIndex(0);
                item.setContent(Map.of(
                                "fileName", fileName,
                                "mimeType", mimeType,
                                "sizeBytes", bytes.length,
                                "base64", Base64.getEncoder().encodeToString(bytes)));

                datasetItemRepository.save(item);
        }

        @Test
        void shouldConfigureSimpleProjectWithLabelsAndGuidelineText() throws Exception {
                createUser("owner.setup@example.com");
                User owner = userRepository.findByEmailIgnoreCase("owner.setup@example.com").orElseThrow();

                ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                Project project = new Project();
                project.setResearchGroup(group);
                project.setName("Setup Project");
                project = projectRepository.save(project);

                String session = loginAs("owner.setup@example.com");

                ProjectSetupRequestDto request = new ProjectSetupRequestDto(
                                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                                List.of(
                                                new ProjectSetupLabelDto("Positivo", null),
                                                new ProjectSetupLabelDto("Negativo", null)),
                                "Etiquetar según polaridad del texto",
                                null,
                                null);

                mockMvc.perform(put("/api/research-groups/{groupId}/projects/{projectId}/setup", group.getId(),
                                project.getId())
                                .header("Authorization", "Bearer " + session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.projectId").value(project.getId()))
                                .andExpect(jsonPath("$.projectType").value("TEXT_CLASSIFICATION_SIMPLE"))
                                .andExpect(jsonPath("$.labels.length()").value(2))
                                .andExpect(jsonPath("$.labels[0].name").value("Positivo"))
                                .andExpect(jsonPath("$.guidelineText").value("Etiquetar según polaridad del texto"))
                                .andExpect(jsonPath("$.setupCompleted").value(true));
        }

        @Test
        void shouldRejectLabelsForSeq2SeqProjectType() throws Exception {
                createUser("owner.seq2seq@example.com");
                User owner = userRepository.findByEmailIgnoreCase("owner.seq2seq@example.com").orElseThrow();

                ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                Project project = new Project();
                project.setResearchGroup(group);
                project.setName("Seq2Seq Setup Project");
                project = projectRepository.save(project);

                String session = loginAs("owner.seq2seq@example.com");

                ProjectSetupRequestDto request = new ProjectSetupRequestDto(
                                ProjectType.SEQ2SEQ,
                                List.of(new ProjectSetupLabelDto("Etiqueta prohibida", null)),
                                "Usar pares source-target",
                                null,
                                null);

                mockMvc.perform(put("/api/research-groups/{groupId}/projects/{projectId}/setup", group.getId(),
                                project.getId())
                                .header("Authorization", "Bearer " + session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldConfigureNerProjectWithColors() throws Exception {
                createUser("owner.ner@example.com");
                User owner = userRepository.findByEmailIgnoreCase("owner.ner@example.com").orElseThrow();

                ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                Project project = new Project();
                project.setResearchGroup(group);
                project.setName("NER Setup Project");
                project = projectRepository.save(project);

                createDatasetItem(project, "dataset.txt", "text/plain", "John works at OpenAI");

                String session = loginAs("owner.ner@example.com");

                ProjectSetupRequestDto request = new ProjectSetupRequestDto(
                                ProjectType.NER,
                                List.of(
                                                new ProjectSetupLabelDto("PERSON", "#10B981"),
                                                new ProjectSetupLabelDto("ORG", "#F43F5E")),
                                "Anotar entidades",
                                null,
                                null);

                mockMvc.perform(put("/api/research-groups/{groupId}/projects/{projectId}/setup", group.getId(),
                                project.getId())
                                .header("Authorization", "Bearer " + session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.projectType").value("NER"))
                                .andExpect(jsonPath("$.labels[0].name").value("PERSON"))
                                .andExpect(jsonPath("$.labels[0].color").value("#10B981"));
        }

        @Test
        void shouldRejectNerProjectWhenDatasetContainsUnsupportedFile() throws Exception {
                createUser("owner.ner.invalid@example.com");
                User owner = userRepository.findByEmailIgnoreCase("owner.ner.invalid@example.com").orElseThrow();

                ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                Project project = new Project();
                project.setResearchGroup(group);
                project.setName("NER Unsupported Dataset Project");
                project = projectRepository.save(project);

                createDatasetItem(project, "dataset.csv", "text/csv", "id,text\n1,alpha");

                String session = loginAs("owner.ner.invalid@example.com");

                ProjectSetupRequestDto request = new ProjectSetupRequestDto(
                                ProjectType.NER,
                                List.of(new ProjectSetupLabelDto("PERSON", "#10B981")),
                                "Anotar entidades",
                                null,
                                null);

                mockMvc.perform(put("/api/research-groups/{groupId}/projects/{projectId}/setup", group.getId(),
                                project.getId())
                                .header("Authorization", "Bearer " + session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRequireAnnotationTargetColumnWhenDatasetIsCsv() throws Exception {
                createUser("owner.csv.target.required@example.com");
                User owner = userRepository.findByEmailIgnoreCase("owner.csv.target.required@example.com")
                                .orElseThrow();

                ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                Project project = new Project();
                project.setResearchGroup(group);
                project.setName("CSV Target Required Project");
                project = projectRepository.save(project);

                createDatasetItem(project, "dataset.csv", "text/csv",
                                "instance_id,text,distil_predictions\n1,alpha,{}\n");

                String session = loginAs("owner.csv.target.required@example.com");

                ProjectSetupRequestDto request = new ProjectSetupRequestDto(
                                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                                List.of(new ProjectSetupLabelDto("Correct", null)),
                                "Validar explicaciones",
                                null,
                                null);

                mockMvc.perform(put("/api/research-groups/{groupId}/projects/{projectId}/setup", group.getId(),
                                project.getId())
                                .header("Authorization", "Bearer " + session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldConfigureCsvSetupWithTargetColumnAndDefaultExportNames() throws Exception {
                createUser("owner.csv.target.valid@example.com");
                User owner = userRepository.findByEmailIgnoreCase("owner.csv.target.valid@example.com").orElseThrow();

                ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                Project project = new Project();
                project.setResearchGroup(group);
                project.setName("CSV Target Valid Project");
                project = projectRepository.save(project);

                createDatasetItem(project, "dataset.csv", "text/csv",
                                "instance_id,text,distil_predictions\n1,alpha,{}\n");

                String session = loginAs("owner.csv.target.valid@example.com");

                ProjectSetupRequestDto request = new ProjectSetupRequestDto(
                                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                                List.of(new ProjectSetupLabelDto("Correct", null)),
                                "Validar explicaciones",
                                null,
                                "distil_predictions");

                mockMvc.perform(put("/api/research-groups/{groupId}/projects/{projectId}/setup", group.getId(),
                                project.getId())
                                .header("Authorization", "Bearer " + session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.projectId").value(project.getId()))
                                .andExpect(jsonPath("$.annotationTargetColumn").value("distil_predictions"));
        }
}
