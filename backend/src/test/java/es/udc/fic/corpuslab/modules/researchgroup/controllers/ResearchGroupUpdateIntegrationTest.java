package es.udc.fic.corpuslab.modules.researchgroup.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import es.udc.fic.corpuslab.AbstractIntegrationTest;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserLoginRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResearchGroupUpdateIntegrationTest extends AbstractIntegrationTest {

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
        private DatasetItemRepository datasetItemRepository;

        @Autowired
        private ProjectParticipantRepository projectParticipantRepository;

        @Autowired
        private ProjectRepository projectRepository;

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

        private String loginAs(String email) throws Exception {
                MvcResult result = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                UserLoginRequestTestBuilder.validRequest()
                                                                .withEmail(email)
                                                                .build())))
                                .andExpect(status().isOk())
                                .andReturn();

                return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        }

        @Test
        void shouldUpdateGroupWhenRequesterIsOwner() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner.update@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                                .withName("Original Name")
                                .withDescription("Original Description")
                                .build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                String ownerSession = loginAs("owner.update@example.com");

                mockMvc.perform(put("/api/research-groups/" + group.getId())
                                .header("Authorization", "Bearer " + ownerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Updated Name\",\"description\":\"Updated Description\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(group.getId()))
                                .andExpect(jsonPath("$.name").value("Updated Name"))
                                .andExpect(jsonPath("$.description").value("Updated Description"));
        }

        @Test
        void shouldTrimNameAndDescriptionOnUpdate() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner.trim@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                String ownerSession = loginAs("owner.trim@example.com");

                mockMvc.perform(put("/api/research-groups/" + group.getId())
                                .header("Authorization", "Bearer " + ownerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"  Trim Name  \",\"description\":\"  Trim Desc  \"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Trim Name"))
                                .andExpect(jsonPath("$.description").value("Trim Desc"));
        }

        @Test
        void shouldSetDescriptionNullWhenBlankDescription() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner.null@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                String ownerSession = loginAs("owner.null@example.com");

                mockMvc.perform(put("/api/research-groups/" + group.getId())
                                .header("Authorization", "Bearer " + ownerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Updated Name\",\"description\":\"   \"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.description").isEmpty());
        }

        @Test
        void shouldReturnForbiddenWhenRequesterIsNotOwner() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner.noedit@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                User admin = UserTestBuilder.validUser()
                                .withEmail("admin.noedit@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                admin = userRepository.save(admin);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(admin)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ADMIN)
                                .build());

                String adminSession = loginAs("admin.noedit@example.com");

                mockMvc.perform(put("/api/research-groups/" + group.getId())
                                .header("Authorization", "Bearer " + adminSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Updated Name\",\"description\":\"Updated Description\"}"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnNotFoundWhenGroupDoesNotExist() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner.notfound@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(owner);

                String ownerSession = loginAs("owner.notfound@example.com");

                mockMvc.perform(put("/api/research-groups/999999")
                                .header("Authorization", "Bearer " + ownerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Updated Name\",\"description\":\"Updated Description\"}"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldRejectBlankNameOnUpdate() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner.validation@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                String ownerSession = loginAs("owner.validation@example.com");

                mockMvc.perform(put("/api/research-groups/" + group.getId())
                                .header("Authorization", "Bearer " + ownerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"   \",\"description\":\"Updated Description\"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectMissingNameOnUpdate() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner.missingname@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                String ownerSession = loginAs("owner.missingname@example.com");

                mockMvc.perform(put("/api/research-groups/" + group.getId())
                                .header("Authorization", "Bearer " + ownerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"description\":\"Updated Description\"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectDescriptionTooLongOnUpdate() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner.longdesc@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                String ownerSession = loginAs("owner.longdesc@example.com");
                String tooLongDescription = "a".repeat(2049);

                mockMvc.perform(put("/api/research-groups/" + group.getId())
                                .header("Authorization", "Bearer " + ownerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Updated Name\",\"description\":\"" + tooLongDescription + "\"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnForbiddenWhenRequesterIsNotMember() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner.membercheck@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                User outsider = UserTestBuilder.validUser()
                                .withEmail("outsider.membercheck@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(outsider);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                String outsiderSession = loginAs("outsider.membercheck@example.com");

                mockMvc.perform(put("/api/research-groups/" + group.getId())
                                .header("Authorization", "Bearer " + outsiderSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Updated Name\",\"description\":\"Updated Description\"}"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
                mockMvc.perform(put("/api/research-groups/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Updated Name\",\"description\":\"Updated Description\"}"))
                                .andExpect(status().isForbidden());
        }
}
