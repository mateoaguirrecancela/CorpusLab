package es.udc.fic.corpuslab.modules.researchgroup.controllers;

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
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupMember;
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

import java.time.Instant;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResearchGroupListIntegrationTest extends AbstractIntegrationTest {

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
        void shouldReturnEmptyListWhenUserHasNoGroups() throws Exception {
                User user = UserTestBuilder.validUser()
                                .withEmail("no.groups@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(user);

                String session = loginAs("no.groups@example.com");

                mockMvc.perform(get("/api/research-groups").header("Authorization", "Bearer " + session))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void shouldReturnGroupsWithRoleAndMemberCount() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                User annotator = UserTestBuilder.validUser()
                                .withEmail("annotator@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                annotator = userRepository.save(annotator);

                ResearchGroup groupA = ResearchGroupTestBuilder.validGroup()
                                .withName("NLP Lab")
                                .withDescription("Natural Language Processing research")
                                .build();
                groupA = researchGroupRepository.save(groupA);

                ResearchGroup groupB = ResearchGroupTestBuilder.validGroup()
                                .withName("AI Ethics")
                                .withDescription("AI Ethics research")
                                .build();
                groupB = researchGroupRepository.save(groupB);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(groupA)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(annotator)
                                .withResearchGroup(groupA)
                                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                                .build());

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(groupB)
                                .withRole(ResearchGroupMemberRole.ADMIN)
                                .build());

                String session = loginAs("owner@example.com");

                mockMvc.perform(get("/api/research-groups").header("Authorization", "Bearer " + session))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].name").value("AI Ethics"))
                                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                                .andExpect(jsonPath("$[0].memberCount").value(1))
                                .andExpect(jsonPath("$[1].name").value("NLP Lab"))
                                .andExpect(jsonPath("$[1].role").value("OWNER"))
                                .andExpect(jsonPath("$[1].memberCount").value(2))
                                .andExpect(jsonPath("$[1].description").value("Natural Language Processing research"))
                                .andExpect(jsonPath("$[1].id").isNumber())
                                .andExpect(jsonPath("$[1].createdAt").isNotEmpty());
        }

        @Test
        void shouldNotReturnGroupsOfOtherUsers() throws Exception {
                User userA = UserTestBuilder.validUser()
                                .withEmail("user.a@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userRepository.save(userA);

                User userB = UserTestBuilder.validUser()
                                .withEmail("user.b@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                userB = userRepository.save(userB);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                                .withName("Secret Lab")
                                .build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(userB)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                String session = loginAs("user.a@example.com");

                mockMvc.perform(get("/api/research-groups").header("Authorization", "Bearer " + session))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void shouldExcludeSoftDeletedMemberships() throws Exception {
                User user = UserTestBuilder.validUser()
                                .withEmail("deleted.member@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                user = userRepository.save(user);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                                .withName("Old Group")
                                .build();
                group = researchGroupRepository.save(group);

                ResearchGroupMember member = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(user)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                                .build();
                member = memberRepository.save(member);

                member.setDeletedAt(Instant.now());
                memberRepository.save(member);

                String session = loginAs("deleted.member@example.com");

                mockMvc.perform(get("/api/research-groups").header("Authorization", "Bearer " + session))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
                mockMvc.perform(get("/api/research-groups"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void shouldNotCountSoftDeletedMembersInMemberCount() throws Exception {
                User owner = UserTestBuilder.validUser()
                                .withEmail("owner.count@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                owner = userRepository.save(owner);

                User activeAnnotator = UserTestBuilder.validUser()
                                .withEmail("active.annotator@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                activeAnnotator = userRepository.save(activeAnnotator);

                User removedAnnotator = UserTestBuilder.validUser()
                                .withEmail("removed.annotator@example.com")
                                .withPasswordHash(passwordEncoder.encode("strong-password"))
                                .build();
                removedAnnotator = userRepository.save(removedAnnotator);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                                .withName("Count Test Group")
                                .build();
                group = researchGroupRepository.save(group);

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build());

                memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                                .withUser(activeAnnotator)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                                .build());

                ResearchGroupMember removed = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(removedAnnotator)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                                .build();
                removed = memberRepository.save(removed);
                removed.setDeletedAt(Instant.now());
                memberRepository.save(removed);

                String session = loginAs("owner.count@example.com");

                mockMvc.perform(get("/api/research-groups").header("Authorization", "Bearer " + session))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].name").value("Count Test Group"))
                                .andExpect(jsonPath("$[0].memberCount").value(2));
        }
}
