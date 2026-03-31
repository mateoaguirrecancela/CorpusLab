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
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupMember;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupMemberTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;

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

import java.time.Instant;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResearchGroupDetailIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResearchGroupRepository researchGroupRepository;

    @Autowired
    private ResearchGroupMemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @BeforeEach
    void cleanData() {
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

    @Test
    void shouldReturnGroupDetailForMember() throws Exception {
        User user = UserTestBuilder.validUser()
                .withEmail("member@example.com")
                .withFirstName("Elena")
                .withLastName("Alvarez")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        user = userRepository.save(user);

        ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                .withName("CITIC - NLP UDC")
                .withDescription("Computational Linguistics Research Group")
                .build();
        group = researchGroupRepository.save(group);

        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(user)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());

        MockHttpSession session = loginAs("member@example.com");

        mockMvc.perform(get("/api/research-groups/" + group.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(group.getId()))
                .andExpect(jsonPath("$.name").value("CITIC - NLP UDC"))
                .andExpect(jsonPath("$.description").value("Computational Linguistics Research Group"))
                .andExpect(jsonPath("$.totalMembers").value(1))
                .andExpect(jsonPath("$.members.length()").value(1))
                .andExpect(jsonPath("$.members[0].firstName").value("Elena"))
                .andExpect(jsonPath("$.members[0].role").value("OWNER"));
    }

    @Test
    void shouldReturnForbiddenForNonMember() throws Exception {
        User userA = UserTestBuilder.validUser()
                .withEmail("usera@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(userA);

        User userB = UserTestBuilder.validUser()
                .withEmail("userb@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userB = userRepository.save(userB);

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
        group = researchGroupRepository.save(group);

        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(userB)
                .withResearchGroup(group)
                .build());

        MockHttpSession session = loginAs("usera@example.com");

        mockMvc.perform(get("/api/research-groups/" + group.getId()).session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundForNonExistentGroup() throws Exception {
        User user = UserTestBuilder.validUser()
                .withEmail("user@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(user);

        MockHttpSession session = loginAs("user@example.com");

        mockMvc.perform(get("/api/research-groups/999").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnForbiddenWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/research-groups/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnDetailWithExpectedContract() throws Exception {
        User owner = UserTestBuilder.validUser()
                .withEmail("owner.contract@example.com")
                .withFirstName("Elena")
                .withLastName("Alvarez")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        owner = userRepository.save(owner);

        User admin = UserTestBuilder.validUser()
                .withEmail("admin.contract@example.com")
                .withFirstName("Linda")
                .withLastName("Vo")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        admin = userRepository.save(admin);

        ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                .withName("Contract Group")
                .withDescription("Contract response validation")
                .build();
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

        MockHttpSession session = loginAs("owner.contract@example.com");

        mockMvc.perform(get("/api/research-groups/" + group.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(group.getId()))
                .andExpect(jsonPath("$.name").value("Contract Group"))
                .andExpect(jsonPath("$.description").value("Contract response validation"))
                .andExpect(jsonPath("$.totalMembers").value(2))
                .andExpect(jsonPath("$.activeProjects").value(0))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.members[0].email").isNotEmpty())
                .andExpect(jsonPath("$.members[0].role").isNotEmpty());
    }

    @Test
    void shouldExcludeSoftDeletedMembersFromDetail() throws Exception {
        User owner = UserTestBuilder.validUser()
                .withEmail("owner.softdelete@example.com")
                .withFirstName("Elena")
                .withLastName("Alvarez")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        owner = userRepository.save(owner);

        User removedMemberUser = UserTestBuilder.validUser()
                .withEmail("removed.member@example.com")
                .withFirstName("Removed")
                .withLastName("Member")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        removedMemberUser = userRepository.save(removedMemberUser);

        ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                .withName("Soft Delete Group")
                .build();
        group = researchGroupRepository.save(group);

        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(owner)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());

        ResearchGroupMember removedMember = ResearchGroupMemberTestBuilder.validMember()
                .withUser(removedMemberUser)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                .build();
        removedMember = memberRepository.save(removedMember);
        removedMember.setDeletedAt(Instant.now());
        memberRepository.save(removedMember);

        MockHttpSession session = loginAs("owner.softdelete@example.com");

        mockMvc.perform(get("/api/research-groups/" + group.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMembers").value(1))
                .andExpect(jsonPath("$.members.length()").value(1))
                .andExpect(jsonPath("$.members[0].email").value("owner.softdelete@example.com"));
    }
}
