package es.udc.fic.corpuslab.modules.researchgroup.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupDetailDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupMemberDto;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;

@ExtendWith(MockitoExtension.class)
class ResearchGroupServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResearchGroupRepository researchGroupRepository;

    @Mock
    private ResearchGroupMemberRepository memberRepository;

    private ResearchGroupService researchGroupService;

    @BeforeEach
    void setUp() {
        researchGroupService = new ResearchGroupServiceImpl(
                userRepository,
                researchGroupRepository,
                memberRepository);
    }

    @Test
    void getResearchGroupDetailShouldReturnDetailWhenRequesterIsMember() {
        User requester = UserTestBuilder.validUser().withEmail("member@example.com").build();
        setId(requester, 10L);

        ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                .withName("CITIC - NLP UDC")
                .withDescription("Computational Linguistics Research Group")
                .build();
        setGroupFields(group, 99L, Instant.parse("2026-03-31T12:00:00Z"));

        ResearchGroupMemberDto owner = new ResearchGroupMemberDto(
                10L,
                "Elena",
                "Alvarez",
                "member@example.com",
                ResearchGroupMemberRole.OWNER);
        ResearchGroupMemberDto admin = new ResearchGroupMemberDto(
                11L,
                "Linda",
                "Vo",
                "admin@example.com",
                ResearchGroupMemberRole.ADMIN);

        when(userRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(requester));
        when(researchGroupRepository.findById(99L)).thenReturn(Optional.of(group));
        when(memberRepository.findMembersByGroupId(99L)).thenReturn(List.of(owner, admin));

        ResearchGroupDetailDto detail = researchGroupService.getResearchGroupDetail("member@example.com", 99L);

        assertThat(detail.id()).isEqualTo(99L);
        assertThat(detail.name()).isEqualTo("CITIC - NLP UDC");
        assertThat(detail.description()).isEqualTo("Computational Linguistics Research Group");
        assertThat(detail.totalMembers()).isEqualTo(2L);
        assertThat(detail.activeProjects()).isEqualTo(0L);
        assertThat(detail.createdAt()).isEqualTo(Instant.parse("2026-03-31T12:00:00Z"));
        assertThat(detail.members()).hasSize(2);
        assertThat(detail.members().get(0).email()).isEqualTo("member@example.com");
    }

    @Test
    void getResearchGroupDetailShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> researchGroupService.getResearchGroupDetail("missing@example.com", 1L))
                .isInstanceOf(EmailNotFoundException.class)
                .hasMessage("No account found for email: missing@example.com");
    }

    @Test
    void getResearchGroupDetailShouldThrowWhenGroupDoesNotExist() {
        User requester = UserTestBuilder.validUser().withEmail("member@example.com").build();
        setId(requester, 10L);

        when(userRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(requester));
        when(researchGroupRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> researchGroupService.getResearchGroupDetail("member@example.com", 404L))
                .isInstanceOf(ResearchGroupNotFoundException.class)
                .hasMessage("Research group not found with id: 404");
    }

    @Test
    void getResearchGroupDetailShouldThrowWhenRequesterIsNotMember() {
        User requester = UserTestBuilder.validUser().withEmail("requester@example.com").build();
        setId(requester, 22L);

        ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
        setGroupFields(group, 50L, Instant.parse("2026-03-31T12:00:00Z"));

        ResearchGroupMemberDto otherMember = new ResearchGroupMemberDto(
                30L,
                "Other",
                "Member",
                "other@example.com",
                ResearchGroupMemberRole.ANNOTATOR);

        when(userRepository.findByEmailIgnoreCase("requester@example.com")).thenReturn(Optional.of(requester));
        when(researchGroupRepository.findById(50L)).thenReturn(Optional.of(group));
        when(memberRepository.findMembersByGroupId(50L)).thenReturn(List.of(otherMember));

        assertThatThrownBy(() -> researchGroupService.getResearchGroupDetail("requester@example.com", 50L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is not a member of this research group");
    }

    private void setId(User user, Long id) {
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void setGroupFields(ResearchGroup group, Long id, Instant createdAt) {
        try {
            java.lang.reflect.Field idField = ResearchGroup.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(group, id);

            java.lang.reflect.Field createdAtField = ResearchGroup.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(group, createdAt);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
