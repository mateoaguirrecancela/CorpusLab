package es.udc.fic.corpuslab.modules.project.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import java.util.List;

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
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupMemberTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileSecurityIntegrationTest extends AbstractIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ResearchGroupRepository researchGroupRepository;

        @Autowired
        private ResearchGroupMemberRepository memberRepository;

        @Autowired
        private ProjectRepository projectRepository;

        @Autowired
        private DatasetItemRepository datasetItemRepository;

        @Autowired
        private ProjectParticipantRepository projectParticipantRepository;

        @Autowired
        private NotificationRepository notificationRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        private String sessionToken;
        private Long groupId;
        private Long projectId;

        @BeforeEach
        void setup() throws Exception {
                notificationRepository.deleteAll();
                datasetItemRepository.deleteAll();
                projectParticipantRepository.deleteAll();
                projectRepository.deleteAll();
                memberRepository.deleteAll();
                researchGroupRepository.deleteAll();
                userRepository.deleteAll();

                String email = "security.admin@example.com";
                User user = UserTestBuilder.validUser()
                                .withEmail(email)
                                .withPasswordHash(passwordEncoder.encode("password"))
                                .build();
                userRepository.save(user);

                ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());
                groupId = group.getId();
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(user)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                Project project = new Project();
                project.setResearchGroup(group);
                project.setName("Security Test Project");
                project = projectRepository.save(project);
                projectId = project.getId();

                MvcResult result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                UserLoginRequestTestBuilder.validRequest().withEmail(email)
                                                                .withPassword("password").build())))
                                .andExpect(status().isOk())
                                .andReturn();
                sessionToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        }

        @Test
        void shouldRejectPathTraversalInFilename() throws Exception {
                MockMultipartFile traversal = new MockMultipartFile(
                                "files",
                                "../../etc/passwd.txt",
                                "text/plain",
                                "dangerous content".getBytes());

                mockMvc.perform(multipart("/api/research-groups/{groupId}/projects/{projectId}/dataset", groupId,
                                projectId)
                                .file(traversal)
                                .header("Authorization", "Bearer " + sessionToken))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value(
                                                "Invalid dataset upload: Invalid filename: potential path traversal"));
        }

        @Test
        void shouldRejectForbiddenExtensions() throws Exception {
                MockMultipartFile exe = new MockMultipartFile(
                                "files",
                                "malware.exe",
                                "application/octet-stream",
                                "fake binary".getBytes());

                mockMvc.perform(multipart("/api/research-groups/{groupId}/projects/{projectId}/dataset", groupId,
                                projectId)
                                .file(exe)
                                .header("Authorization", "Bearer " + sessionToken))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message")
                                                .value("Invalid dataset upload: Unsupported file type: exe"));
        }

        @Test
        void shouldRejectSpoofedBinaryFilesUsingMagicBytes() throws Exception {
                // MZ header is used for Windows executables
                byte[] exeContent = new byte[100];
                exeContent[0] = 'M';
                exeContent[1] = 'Z';

                MockMultipartFile spoofed = new MockMultipartFile(
                                "files",
                                "not-an-exe.txt",
                                "text/plain",
                                exeContent);

                mockMvc.perform(multipart("/api/research-groups/{groupId}/projects/{projectId}/dataset", groupId,
                                projectId)
                                .file(spoofed)
                                .header("Authorization", "Bearer " + sessionToken))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value(
                                                "Invalid dataset upload: Binary/Executable files are not allowed"));
        }

        @Test
        void shouldSanitizeCsvInjectionVectors() throws Exception {
                String dangerousCsv = "name,score\nJohn,=1+2\nJane,-500\nBob,@SUM(A1:A2)";
                MockMultipartFile csv = new MockMultipartFile(
                                "files",
                                "test.csv",
                                "text/csv",
                                dangerousCsv.getBytes());

                mockMvc.perform(multipart("/api/research-groups/{groupId}/projects/{projectId}/dataset", groupId,
                                projectId)
                                .file(csv)
                                .header("Authorization", "Bearer " + sessionToken))
                                .andExpect(status().isAccepted());

                // Verify content in DB
                var items = waitForDatasetItems();
                String base64 = (String) items.get(0).getContent().get("base64");
                String sanitizedContent = new String(Base64.getDecoder().decode(base64));

                // Note: The sanitization adds a single quote before the injection characters
                // Our logic currently splits by comma and checks startsWith
                // "John,=1+2" -> "John,'=1+2"
                org.junit.jupiter.api.Assertions.assertTrue(sanitizedContent.contains("John,'=1+2"));
                org.junit.jupiter.api.Assertions.assertTrue(sanitizedContent.contains("Jane,'-500"));
                org.junit.jupiter.api.Assertions.assertTrue(sanitizedContent.contains("Bob,'@SUM(A1:A2)"));
        }

        @Test
        void shouldDeriveStoredMimeTypeFromExtensionIgnoringDeclaredContentType() throws Exception {
                // A declared text/html would be echoed back on an inline response and execute
                // in the app origin, so the stored type must come from the extension instead.
                MockMultipartFile spoofedHtml = new MockMultipartFile(
                                "files",
                                "notes.txt",
                                "text/html",
                                "<script>alert(document.domain)</script>".getBytes());

                mockMvc.perform(multipart("/api/research-groups/{groupId}/projects/{projectId}/dataset", groupId,
                                projectId)
                                .file(spoofedHtml)
                                .header("Authorization", "Bearer " + sessionToken))
                                .andExpect(status().isAccepted());

                var items = waitForDatasetItems();
                org.junit.jupiter.api.Assertions.assertEquals(
                                "text/plain",
                                items.get(0).getContent().get("mimeType"));
        }

        private List<DatasetItem> waitForDatasetItems()
                        throws InterruptedException {
                for (int attempt = 0; attempt < 20; attempt++) {
                        var items = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);
                        if (!items.isEmpty()) {
                                return items;
                        }
                        Thread.sleep(250L);
                }
                return datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);
        }
}
