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
import org.springframework.mock.web.MockHttpSession;
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
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupMemberTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupInvitationRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;

import java.util.List;

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
        private PasswordEncoder passwordEncoder;

        private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

        @BeforeEach
        void cleanData() {
                notificationRepository.deleteAll();
                invitationRepository.deleteAll();
                memberRepository.deleteAll();
                datasetItemRepository.deleteAll();
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

        private MockHttpSession loginAs(String email) throws Exception {
                MvcResult result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                UserLoginRequestTestBuilder.validRequest().withEmail(email).build())))
                                .andExpect(status().isOk())
                                .andReturn();

                return (MockHttpSession) result.getRequest().getSession(false);
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

                MockHttpSession session = loginAs("owner.setup@example.com");

                ProjectSetupRequestDto request = new ProjectSetupRequestDto(
                                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                                List.of(
                                                new ProjectSetupLabelDto("Positivo", null),
                                                new ProjectSetupLabelDto("Negativo", null)),
                                "Etiquetar según polaridad del texto",
                                null);

                mockMvc.perform(put("/api/research-groups/{groupId}/projects/{projectId}/setup", group.getId(),
                                project.getId())
                                .session(session)
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

                MockHttpSession session = loginAs("owner.seq2seq@example.com");

                ProjectSetupRequestDto request = new ProjectSetupRequestDto(
                                ProjectType.SEQ2SEQ,
                                List.of(new ProjectSetupLabelDto("Etiqueta prohibida", null)),
                                "Usar pares source-target",
                                null);

                mockMvc.perform(put("/api/research-groups/{groupId}/projects/{projectId}/setup", group.getId(),
                                project.getId())
                                .session(session)
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

                MockHttpSession session = loginAs("owner.ner@example.com");

                ProjectSetupRequestDto request = new ProjectSetupRequestDto(
                                ProjectType.NER,
                                List.of(
                                                new ProjectSetupLabelDto("PERSON", "#10B981"),
                                                new ProjectSetupLabelDto("ORG", "#F43F5E")),
                                "Anotar entidades",
                                null);

                mockMvc.perform(put("/api/research-groups/{groupId}/projects/{projectId}/setup", group.getId(),
                                project.getId())
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.projectType").value("NER"))
                                .andExpect(jsonPath("$.labels[0].name").value("PERSON"))
                                .andExpect(jsonPath("$.labels[0].color").value("#10B981"));
        }
}
