package es.udc.fic.corpuslab.modules.researchgroup.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import es.udc.fic.corpuslab.AbstractIntegrationTest;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserLoginRequestTestBuilder;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import es.udc.fic.corpuslab.modules.project.entities.Project;
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
class ResearchGroupDeleteIntegrationTest extends AbstractIntegrationTest {

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
    void shouldDeleteGroupWhenRequesterIsOwner() throws Exception {
        User owner = UserTestBuilder.validUser()
                .withEmail("owner.delete@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        owner = userRepository.save(owner);

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());

        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(owner)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());

        String session = loginAs("owner.delete@example.com");

        mockMvc.perform(delete("/api/research-groups/" + group.getId())
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isNoContent());

        assertThat(researchGroupRepository.existsById(group.getId())).isFalse();
    }

    @Test
    void shouldReturnForbiddenWhenRequesterIsNotOwner() throws Exception {
        User admin = UserTestBuilder.validUser()
                .withEmail("admin.delete@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        admin = userRepository.save(admin);

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());

        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(admin)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.ADMIN)
                .build());

        String session = loginAs("admin.delete@example.com");

        mockMvc.perform(delete("/api/research-groups/" + group.getId())
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isForbidden());

        assertThat(researchGroupRepository.existsById(group.getId())).isTrue();
    }

    @Test
    void shouldReturnNotFoundWhenGroupDoesNotExist() throws Exception {
        User owner = UserTestBuilder.validUser()
                .withEmail("owner.notfound@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        userRepository.save(owner);

        String session = loginAs("owner.notfound@example.com");

        mockMvc.perform(delete("/api/research-groups/999999")
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteMembersAndProjectsWhenGroupIsDeleted() throws Exception {
        User owner = UserTestBuilder.validUser()
                .withEmail("owner.cascade@example.com")
                .withPasswordHash(passwordEncoder.encode("strong-password"))
                .build();
        owner = userRepository.save(owner);

        ResearchGroup group = researchGroupRepository.save(ResearchGroupTestBuilder.validGroup().build());

        memberRepository.save(ResearchGroupMemberTestBuilder.validMember()
                .withUser(owner)
                .withResearchGroup(group)
                .withRole(ResearchGroupMemberRole.OWNER)
                .build());

        Project project = new Project();
        project.setName("Project to delete");
        project.setResearchGroup(group);
        project = projectRepository.save(project);

        String session = loginAs("owner.cascade@example.com");

        mockMvc.perform(delete("/api/research-groups/" + group.getId())
                .header("Authorization", "Bearer " + session))
                .andExpect(status().isNoContent());

        assertThat(researchGroupRepository.existsById(group.getId())).isFalse();
        assertThat(memberRepository.findByResearchGroupId(group.getId())).isEmpty();
        assertThat(projectRepository.findByResearchGroupId(group.getId())).isEmpty();
    }
}
