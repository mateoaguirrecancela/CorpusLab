package es.udc.fic.corpuslab.modules.researchgroup.controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import es.udc.fic.corpuslab.AbstractIntegrationTest;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserLoginRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupInvitation;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupInvitationStatus;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupMemberTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupInvitationRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;
import es.udc.fic.corpuslab.modules.notification.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResearchGroupInvitationIntegrationTest extends AbstractIntegrationTest {

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
        private PasswordEncoder passwordEncoder;

        @MockitoBean
        private EmailService emailService;

        private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

        @BeforeEach
        void cleanData() {
                notificationRepository.deleteAll();
                invitationRepository.deleteAll();
                memberRepository.deleteAll();
                researchGroupRepository.deleteAll();
                userRepository.deleteAll();
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

        private String invitationPayload(String email, ResearchGroupMemberRole role, Instant expiresAt) {
                return String.format("{\"email\":\"%s\",\"role\":\"%s\",\"expiresAt\":\"%s\"}",
                                email,
                                role.name(),
                                expiresAt.toString());
        }

        @Test
        void shouldInviteExistingUserAndShowInvitationInTheirList() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner@example.com")
                                .withFirstName("Owner")
                                .withLastName("Admin")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                User invitee = UserTestBuilder.validUser()
                                .withEmail("invitee@example.com")
                                .withFirstName("Invited")
                                .withLastName("Researcher")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(invitee);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                                .withName("NLP Group")
                                .build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                MockHttpSession ownerSession = loginAs("owner@example.com");
                Instant expiresAt = Instant.now().plusSeconds(86400);

                mockMvc.perform(post("/api/research-groups/" + group.getId() + "/invitations")
                                .session(ownerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invitationPayload("invitee@example.com", ResearchGroupMemberRole.ANNOTATOR,
                                                expiresAt)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.researchGroupId").value(group.getId()))
                                .andExpect(jsonPath("$.researchGroupName").value("NLP Group"))
                                .andExpect(jsonPath("$.invitedEmail").value("invitee@example.com"))
                                .andExpect(jsonPath("$.role").value("ANNOTATOR"))
                                .andExpect(jsonPath("$.status").value("PENDING"));

                verify(emailService).sendResearchGroupInvitationToExistingUser(
                                eq("invitee@example.com"),
                                eq("NLP Group"),
                                eq("Owner Admin"),
                                anyString());

                MockHttpSession inviteeSession = loginAs("invitee@example.com");

                mockMvc.perform(get("/api/research-groups/my-invitations").session(inviteeSession))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].researchGroupName").value("NLP Group"))
                                .andExpect(jsonPath("$[0].invitedEmail").value("invitee@example.com"))
                                .andExpect(jsonPath("$[0].role").value("ANNOTATOR"))
                                .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        void shouldInviteEmailWithoutAccountAndSendSignupEmail() throws Exception {
                User admin = UserTestBuilder.validUser()
                                .withEmail("admin@example.com")
                                .withFirstName("Admin")
                                .withLastName("Leader")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                admin = userRepository.save(admin);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                                .withName("Corpus Group")
                                .build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(admin)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ADMIN)
                                .build());

                MockHttpSession adminSession = loginAs("admin@example.com");
                Instant expiresAt = Instant.now().plusSeconds(86400);

                mockMvc.perform(post("/api/research-groups/" + group.getId() + "/invitations")
                                .session(adminSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invitationPayload("new.researcher@example.com", ResearchGroupMemberRole.ADMIN,
                                                expiresAt)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.invitedEmail").value("new.researcher@example.com"))
                                .andExpect(jsonPath("$.role").value("ADMIN"));

                verify(emailService).sendResearchGroupInvitationToNewUser(
                                eq("new.researcher@example.com"),
                                eq("Corpus Group"),
                                eq("Admin Leader"),
                                anyString());
        }

        @Test
        void shouldReturnConflictWhenInvitationAlreadyExists() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner2@example.com")
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

                ResearchGroupInvitation invitation = new ResearchGroupInvitation();
                invitation.setResearchGroup(group);
                invitation.setInviterUser(owner);
                invitation.setInvitedEmail("duplicate@example.com");
                invitation.setToken("token-duplicate");
                invitation.setRole(ResearchGroupMemberRole.ANNOTATOR);
                invitation.setStatus(ResearchGroupInvitationStatus.PENDING);
                invitation.setExpiresAt(Instant.now().plusSeconds(86400));
                invitationRepository.save(invitation);

                MockHttpSession ownerSession = loginAs("owner2@example.com");
                Instant expiresAt = Instant.now().plusSeconds(86400);

                mockMvc.perform(post("/api/research-groups/" + group.getId() + "/invitations")
                                .session(ownerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invitationPayload("duplicate@example.com", ResearchGroupMemberRole.ANNOTATOR,
                                                expiresAt)))
                                .andExpect(status().isConflict());
        }

        @Test
        void shouldReturnConflictWhenInvitedEmailAlreadyBelongsToMember() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner3@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                User existingMember = UserTestBuilder.validUser()
                                .withEmail("member@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                existingMember = userRepository.save(existingMember);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(existingMember)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                                .build());

                MockHttpSession ownerSession = loginAs("owner3@example.com");
                Instant expiresAt = Instant.now().plusSeconds(86400);

                mockMvc.perform(post("/api/research-groups/" + group.getId() + "/invitations")
                                .session(ownerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invitationPayload("member@example.com", ResearchGroupMemberRole.ANNOTATOR,
                                                expiresAt)))
                                .andExpect(status().isConflict());
        }

        @Test
        void shouldReturnForbiddenWhenAnnotatorTriesToInvite() throws Exception {
                User annotator = UserTestBuilder.validUser()
                                .withEmail("annotator@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                annotator = userRepository.save(annotator);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(annotator)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                                .build());

                MockHttpSession session = loginAs("annotator@example.com");
                Instant expiresAt = Instant.now().plusSeconds(86400);

                mockMvc.perform(post("/api/research-groups/" + group.getId() + "/invitations")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invitationPayload("target@example.com", ResearchGroupMemberRole.ANNOTATOR,
                                                expiresAt)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnServiceUnavailableWhenInvitationEmailFails() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner4@example.com")
                                .withFirstName("Owner")
                                .withLastName("Failure")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                User invitee = UserTestBuilder.validUser()
                                .withEmail("invitee2@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(invitee);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                                .withName("Failure Group")
                                .build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                doThrow(new RuntimeException("smtp down"))
                                .when(emailService)
                                .sendResearchGroupInvitationToExistingUser(eq("invitee2@example.com"),
                                                eq("Failure Group"),
                                                eq("Owner Failure"), anyString());

                MockHttpSession ownerSession = loginAs("owner4@example.com");
                Instant expiresAt = Instant.now().plusSeconds(86400);

                mockMvc.perform(post("/api/research-groups/" + group.getId() + "/invitations")
                                .session(ownerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invitationPayload("invitee2@example.com", ResearchGroupMemberRole.ANNOTATOR,
                                                expiresAt)))
                                .andExpect(status().isServiceUnavailable());
        }

        @Test
        void shouldJoinResearchGroupByCodeAsAnnotator() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner5@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                User joiner = UserTestBuilder.validUser()
                                .withEmail("joiner@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(joiner);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                                .withName("Join Group")
                                .build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                MockHttpSession joinerSession = loginAs("joiner@example.com");

                mockMvc.perform(post("/api/research-groups/join-by-code")
                                .session(joinerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"" + group.getInvitationCode() + "\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(group.getId()))
                                .andExpect(jsonPath("$.role").value("ANNOTATOR"));
        }

        @Test
        void shouldReturnConflictWhenJoiningByCodeAndAlreadyMember() throws Exception {
                User existingMember = UserTestBuilder.validUser()
                                .withEmail("member.join@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                existingMember = userRepository.save(existingMember);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(existingMember)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ADMIN)
                                .build());

                MockHttpSession session = loginAs("member.join@example.com");

                mockMvc.perform(post("/api/research-groups/join-by-code")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"" + group.getInvitationCode() + "\"}"))
                                .andExpect(status().isConflict());
        }

        @Test
        void shouldReturnNotFoundWhenJoiningByUnknownCode() throws Exception {
                User user = UserTestBuilder.validUser()
                                .withEmail("unknown.code@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(user);

                MockHttpSession session = loginAs("unknown.code@example.com");

                mockMvc.perform(post("/api/research-groups/join-by-code")
                                .session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"DOESNOTEXIST\"}"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldAcceptInvitationAndRemoveItFromPendingList() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner6@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                User invitee = UserTestBuilder.validUser()
                                .withEmail("invitee.accept@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                invitee = userRepository.save(invitee);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                                .withName("Accept Group")
                                .build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                ResearchGroupInvitation invitation = new ResearchGroupInvitation();
                invitation.setResearchGroup(group);
                invitation.setInviterUser(owner);
                invitation.setInvitedUser(invitee);
                invitation.setInvitedEmail("invitee.accept@example.com");
                invitation.setToken("accept-token");
                invitation.setRole(ResearchGroupMemberRole.ADMIN);
                invitation.setStatus(ResearchGroupInvitationStatus.PENDING);
                invitation.setExpiresAt(Instant.now().plusSeconds(86400));
                invitation = invitationRepository.save(invitation);

                MockHttpSession inviteeSession = loginAs("invitee.accept@example.com");

                mockMvc.perform(post("/api/research-groups/my-invitations/" + invitation.getId() + "/accept")
                                .session(inviteeSession)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(group.getId()))
                                .andExpect(jsonPath("$.role").value("ADMIN"));

                mockMvc.perform(get("/api/research-groups/my-invitations").session(inviteeSession))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));

                assertThat(memberRepository.findActiveMemberByGroupIdAndUserId(group.getId(), invitee.getId()))
                                .isPresent();
        }

        @Test
        void shouldDeclineInvitationAndRemoveItFromPendingList() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner7@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                User invitee = UserTestBuilder.validUser()
                                .withEmail("invitee.decline@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                invitee = userRepository.save(invitee);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                                .withName("Decline Group")
                                .build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                ResearchGroupInvitation invitation = new ResearchGroupInvitation();
                invitation.setResearchGroup(group);
                invitation.setInviterUser(owner);
                invitation.setInvitedUser(invitee);
                invitation.setInvitedEmail("invitee.decline@example.com");
                invitation.setToken("decline-token");
                invitation.setRole(ResearchGroupMemberRole.ANNOTATOR);
                invitation.setStatus(ResearchGroupInvitationStatus.PENDING);
                invitation.setExpiresAt(Instant.now().plusSeconds(86400));
                invitation = invitationRepository.save(invitation);

                MockHttpSession inviteeSession = loginAs("invitee.decline@example.com");

                mockMvc.perform(post("/api/research-groups/my-invitations/" + invitation.getId() + "/decline")
                                .session(inviteeSession)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNoContent());

                mockMvc.perform(get("/api/research-groups/my-invitations").session(inviteeSession))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));

                assertThat(memberRepository.findActiveMemberByGroupIdAndUserId(group.getId(), invitee.getId()))
                                .isEmpty();
        }

        @Test
        void shouldReturnNotFoundWhenAcceptingUnknownInvitationId() throws Exception {
                User invitee = UserTestBuilder.validUser()
                                .withEmail("invitee.notfound@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(invitee);

                MockHttpSession inviteeSession = loginAs("invitee.notfound@example.com");

                mockMvc.perform(post("/api/research-groups/my-invitations/999999/accept")
                                .session(inviteeSession)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldResolvePendingInvitationWhenJoiningByCode() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner8@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                User joiner = UserTestBuilder.validUser()
                                .withEmail("joiner.pending@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                joiner = userRepository.save(joiner);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                                .withName("Join Clears Invitation Group")
                                .build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                ResearchGroupInvitation invitation = new ResearchGroupInvitation();
                invitation.setResearchGroup(group);
                invitation.setInviterUser(owner);
                invitation.setInvitedUser(joiner);
                invitation.setInvitedEmail("joiner.pending@example.com");
                invitation.setToken("join-pending-token");
                invitation.setRole(ResearchGroupMemberRole.ADMIN);
                invitation.setStatus(ResearchGroupInvitationStatus.PENDING);
                invitation.setExpiresAt(Instant.now().plusSeconds(86400));
                invitation = invitationRepository.save(invitation);

                MockHttpSession joinerSession = loginAs("joiner.pending@example.com");

                mockMvc.perform(post("/api/research-groups/join-by-code")
                                .session(joinerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"" + group.getInvitationCode() + "\"}"))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/research-groups/my-invitations").session(joinerSession))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));

                ResearchGroupInvitation updatedInvitation = invitationRepository.findById(invitation.getId())
                                .orElseThrow();
                assertThat(updatedInvitation.getStatus()).isEqualTo(ResearchGroupInvitationStatus.ACCEPTED);
        }

        @Test
        void shouldUpdateMemberRoleWhenRequesterIsOwner() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner9@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                User member = UserTestBuilder.validUser()
                                .withEmail("member.role@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                member = userRepository.save(member);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("Role Group").build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(member)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                                .build());

                MockHttpSession ownerSession = loginAs("owner9@example.com");

                mockMvc.perform(post("/api/research-groups/" + group.getId() + "/members/" + member.getId() + "/role")
                                .session(ownerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"ADMIN\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(member.getId()))
                                .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        void shouldReturnForbiddenWhenNonOwnerUpdatesMemberRole() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner10@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                User admin = UserTestBuilder.validUser()
                                .withEmail("admin.role@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                admin = userRepository.save(admin);

                User member = UserTestBuilder.validUser()
                                .withEmail("member.role2@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                member = userRepository.save(member);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("Role Group 2").build();
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
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(member)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                                .build());

                MockHttpSession adminSession = loginAs("admin.role@example.com");

                mockMvc.perform(post("/api/research-groups/" + group.getId() + "/members/" + member.getId() + "/role")
                                .session(adminSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"ADMIN\"}"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldRemoveMemberWhenRequesterIsOwner() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner11@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                User member = UserTestBuilder.validUser()
                                .withEmail("member.remove@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                member = userRepository.save(member);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("Remove Group").build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(member)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                                .build());

                MockHttpSession ownerSession = loginAs("owner11@example.com");

                mockMvc.perform(post("/api/research-groups/" + group.getId() + "/members/" + member.getId() + "/remove")
                                .session(ownerSession)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNoContent());

                mockMvc.perform(get("/api/research-groups/" + group.getId()).session(ownerSession))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.members.length()").value(1));
        }

        @Test
        void shouldReturnForbiddenWhenNonOwnerRemovesMember() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner12@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                User admin = UserTestBuilder.validUser()
                                .withEmail("admin.remove@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                admin = userRepository.save(admin);

                User member = UserTestBuilder.validUser()
                                .withEmail("member.remove2@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                member = userRepository.save(member);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("Remove Group 2").build();
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
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(member)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                                .build());

                MockHttpSession adminSession = loginAs("admin.remove@example.com");

                mockMvc.perform(post("/api/research-groups/" + group.getId() + "/members/" + member.getId() + "/remove")
                                .session(adminSession)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnBadRequestWhenUpdatingMemberRoleToOwner() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner13@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                User member = UserTestBuilder.validUser()
                                .withEmail("member.role3@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                member = userRepository.save(member);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("Role Group 3").build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(member)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ADMIN)
                                .build());

                MockHttpSession ownerSession = loginAs("owner13@example.com");

                mockMvc.perform(post("/api/research-groups/" + group.getId() + "/members/" + member.getId() + "/role")
                                .session(ownerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"OWNER\"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnBadRequestWhenRemovingOwnerMember() throws Exception {
                User ownerRequester = UserTestBuilder.validUser()
                                .withEmail("owner14@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                ownerRequester = userRepository.save(ownerRequester);

                User ownerTarget = UserTestBuilder.validUser()
                                .withEmail("owner.target@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                ownerTarget = userRepository.save(ownerTarget);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("Owner Remove Group").build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(ownerRequester)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());
                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(ownerTarget)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                MockHttpSession ownerSession = loginAs("owner14@example.com");

                mockMvc.perform(post(
                                "/api/research-groups/" + group.getId() + "/members/" + ownerTarget.getId() + "/remove")
                                .session(ownerSession)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnNotFoundWhenUpdatingMissingMemberRole() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner15@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("Missing Role Group").build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                MockHttpSession ownerSession = loginAs("owner15@example.com");

                mockMvc.perform(post("/api/research-groups/" + group.getId() + "/members/999999/role")
                                .session(ownerSession)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"ADMIN\"}"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnNotFoundWhenRemovingMissingMember() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner16@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("Missing Remove Group").build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                MockHttpSession ownerSession = loginAs("owner16@example.com");

                mockMvc.perform(post("/api/research-groups/" + group.getId() + "/members/999999/remove")
                                .session(ownerSession)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnForbiddenWhenNotAuthenticatedOnInvitationEndpoints() throws Exception {
                mockMvc.perform(post("/api/research-groups/1/invitations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                                "{\"email\":\"user@example.com\",\"role\":\"ANNOTATOR\",\"expiresAt\":\"2099-01-01T00:00:00Z\"}"))
                                .andExpect(status().isForbidden());

                mockMvc.perform(get("/api/research-groups/my-invitations"))
                                .andExpect(status().isForbidden());

                mockMvc.perform(post("/api/research-groups/join-by-code")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"SOMECODE123\"}"))
                                .andExpect(status().isForbidden());

                mockMvc.perform(post("/api/research-groups/1/members/2/role")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"ADMIN\"}"))
                                .andExpect(status().isForbidden());

                mockMvc.perform(post("/api/research-groups/1/members/2/remove")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden());
        }
}
