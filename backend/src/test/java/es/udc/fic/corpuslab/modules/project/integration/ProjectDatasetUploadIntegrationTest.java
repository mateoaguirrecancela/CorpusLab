package es.udc.fic.corpuslab.modules.project.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.mock.web.MockMultipartFile;
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
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
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
class ProjectDatasetUploadIntegrationTest extends AbstractIntegrationTest {

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

        @Test
        void shouldUploadDatasetFilesWhenRequesterIsOwner() throws Exception {
                createUser("owner.dataset@example.com");
                User owner = userRepository.findByEmailIgnoreCase("owner.dataset@example.com").orElseThrow();

                ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                Project project = new Project();
                project.setResearchGroup(group);
                project.setName("Dataset Project");
                project.setDescription("For dataset upload");
                project = projectRepository.save(project);

                String session = loginAs("owner.dataset@example.com");

                MockMultipartFile txt1 = new MockMultipartFile(
                                "files",
                                "notes.txt",
                                MediaType.TEXT_PLAIN_VALUE,
                                "hello dataset".getBytes());

                MockMultipartFile txt2 = new MockMultipartFile(
                                "files",
                                "items.txt",
                                MediaType.TEXT_PLAIN_VALUE,
                                "more data".getBytes());

                mockMvc.perform(multipart("/api/research-groups/{groupId}/projects/{projectId}/dataset", group.getId(),
                                project.getId())
                                .file(txt1)
                                .file(txt2)
                                .header("Authorization", "Bearer " + session))
                                .andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.jobId").isNotEmpty());
        }

        @Test
        void shouldReturnForbiddenWhenRequesterIsAnnotator() throws Exception {
                createUser("annotator.dataset@example.com");
                User annotator = userRepository.findByEmailIgnoreCase("annotator.dataset@example.com").orElseThrow();

                ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(annotator)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                                .build());

                Project project = new Project();
                project.setResearchGroup(group);
                project.setName("Dataset Project");
                project = projectRepository.save(project);

                String session = loginAs("annotator.dataset@example.com");

                MockMultipartFile txt = new MockMultipartFile(
                                "files",
                                "notes.txt",
                                MediaType.TEXT_PLAIN_VALUE,
                                "hello dataset".getBytes());

                mockMvc.perform(multipart("/api/research-groups/{groupId}/projects/{projectId}/dataset", group.getId(),
                                project.getId())
                                .file(txt)
                                .header("Authorization", "Bearer " + session))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnBadRequestWhenNoFilesAreProvided() throws Exception {
                createUser("owner.empty@example.com");
                User owner = userRepository.findByEmailIgnoreCase("owner.empty@example.com").orElseThrow();

                ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                Project project = new Project();
                project.setResearchGroup(group);
                project.setName("Dataset Project");
                project = projectRepository.save(project);

                String session = loginAs("owner.empty@example.com");

                mockMvc.perform(multipart("/api/research-groups/{groupId}/projects/{projectId}/dataset", group.getId(),
                                project.getId())
                                .header("Authorization", "Bearer " + session))
                                .andExpect(status().isBadRequest());
        }
}
