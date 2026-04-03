package es.udc.fic.corpuslab.modules.researchgroup.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.notification.services.EmailService;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupDetailDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupInvitationDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupMemberDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupSummaryDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.UpdateResearchGroupRequestDto;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupInvitation;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupMember;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupInvitationStatus;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.InvalidResearchGroupInvitationRoleException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.InvalidResearchGroupMemberRoleException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupInvitationAlreadyExistsException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupInvitationCodeNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupInvitationNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupMemberAlreadyExistsException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupMemberNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupMemberTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.fixtures.ResearchGroupTestBuilder;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupInvitationRepository;
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

        @Mock
        private ResearchGroupInvitationRepository invitationRepository;

        @Mock
        private EmailService emailService;

        private ResearchGroupService researchGroupService;

        @BeforeEach
        void setUp() {
                researchGroupService = new ResearchGroupServiceImpl(
                                userRepository,
                                researchGroupRepository,
                                memberRepository,
                                invitationRepository,
                                emailService,
                                "http://localhost:5173");
        }

        @Test
        void getResearchGroupDetailShouldReturnDetailWhenRequesterIsMember() {
                User requester = UserTestBuilder.validUser().withEmail("member@example.com").build();
                setId(requester, 10L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                                .withName("CITIC - NLP UDC")
                                .withDescription("Computational Linguistics Research Group")
                                .build();
                setGroupFields(group, 99L, Instant.parse("2026-03-31T12:00:00Z"), "ABC123XYZ789");

                ResearchGroupMemberDto owner = new ResearchGroupMemberDto(
                                10L,
                                "Elena",
                                "Alvarez",
                                "member@example.com",
                                ResearchGroupMemberRole.OWNER);

                when(userRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(requester));
                when(researchGroupRepository.findById(99L)).thenReturn(Optional.of(group));
                when(memberRepository.findMembersByGroupId(99L)).thenReturn(List.of(owner));

                ResearchGroupDetailDto detail = researchGroupService.getResearchGroupDetail("member@example.com", 99L);

                assertThat(detail.id()).isEqualTo(99L);
                assertThat(detail.invitationCode()).isEqualTo("ABC123XYZ789");
                assertThat(detail.totalMembers()).isEqualTo(1L);
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
        void updateResearchGroupShouldUpdateNameAndDescriptionWhenRequesterIsOwner() {
                User owner = UserTestBuilder.validUser().withEmail("owner@example.com").build();
                setId(owner, 1L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup()
                                .withName("Old Name")
                                .withDescription("Old Description")
                                .build();
                setGroupFields(group, 10L, Instant.parse("2026-03-31T12:00:00Z"), "GROUPCODE001");

                ResearchGroupMember ownerMembership = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build();

                ResearchGroupMemberDto ownerDto = new ResearchGroupMemberDto(
                                1L,
                                "Owner",
                                "User",
                                "owner@example.com",
                                ResearchGroupMemberRole.OWNER);

                when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
                when(researchGroupRepository.findById(10L)).thenReturn(Optional.of(group));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 1L))
                                .thenReturn(Optional.of(ownerMembership));
                when(memberRepository.findMembersByGroupId(10L)).thenReturn(List.of(ownerDto));

                ResearchGroupDetailDto result = researchGroupService.updateResearchGroup(
                                "owner@example.com",
                                10L,
                                new UpdateResearchGroupRequestDto("  New Name  ", "  New Description  "));

                assertThat(result.name()).isEqualTo("New Name");
                assertThat(result.description()).isEqualTo("New Description");
                assertThat(group.getName()).isEqualTo("New Name");
                assertThat(group.getDescription()).isEqualTo("New Description");
                verify(researchGroupRepository).save(group);
        }

        @Test
        void updateResearchGroupShouldSetDescriptionToNullWhenBlank() {
                User owner = UserTestBuilder.validUser().withEmail("owner@example.com").build();
                setId(owner, 1L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                setGroupFields(group, 10L, Instant.parse("2026-03-31T12:00:00Z"), "GROUPCODE001");

                ResearchGroupMember ownerMembership = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build();

                ResearchGroupMemberDto ownerDto = new ResearchGroupMemberDto(
                                1L,
                                "Owner",
                                "User",
                                "owner@example.com",
                                ResearchGroupMemberRole.OWNER);

                when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
                when(researchGroupRepository.findById(10L)).thenReturn(Optional.of(group));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 1L))
                                .thenReturn(Optional.of(ownerMembership));
                when(memberRepository.findMembersByGroupId(10L)).thenReturn(List.of(ownerDto));

                ResearchGroupDetailDto result = researchGroupService.updateResearchGroup(
                                "owner@example.com",
                                10L,
                                new UpdateResearchGroupRequestDto("Updated Name", "   "));

                assertThat(result.description()).isNull();
                assertThat(group.getDescription()).isNull();
        }

        @Test
        void updateResearchGroupShouldThrowWhenRequesterIsNotOwner() {
                User admin = UserTestBuilder.validUser().withEmail("admin@example.com").build();
                setId(admin, 1L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                setGroupFields(group, 10L, Instant.parse("2026-03-31T12:00:00Z"), "GROUPCODE001");

                ResearchGroupMember adminMembership = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(admin)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ADMIN)
                                .build();

                when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
                when(researchGroupRepository.findById(10L)).thenReturn(Optional.of(group));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 1L))
                                .thenReturn(Optional.of(adminMembership));

                assertThatThrownBy(() -> researchGroupService.updateResearchGroup(
                                "admin@example.com",
                                10L,
                                new UpdateResearchGroupRequestDto("Updated Name", "Updated Description")))
                                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void updateResearchGroupShouldThrowWhenGroupNotFound() {
                User owner = UserTestBuilder.validUser().withEmail("owner@example.com").build();
                setId(owner, 1L);

                when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
                when(researchGroupRepository.findById(999L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> researchGroupService.updateResearchGroup(
                                "owner@example.com",
                                999L,
                                new UpdateResearchGroupRequestDto("Updated Name", "Updated Description")))
                                .isInstanceOf(ResearchGroupNotFoundException.class);
        }

        @Test
        void updateResearchGroupShouldThrowWhenRequesterIsNotMember() {
                User requester = UserTestBuilder.validUser().withEmail("outsider@example.com").build();
                setId(requester, 7L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                setGroupFields(group, 10L, Instant.parse("2026-03-31T12:00:00Z"), "GROUPCODE001");

                when(userRepository.findByEmailIgnoreCase("outsider@example.com")).thenReturn(Optional.of(requester));
                when(researchGroupRepository.findById(10L)).thenReturn(Optional.of(group));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 7L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> researchGroupService.updateResearchGroup(
                                "outsider@example.com",
                                10L,
                                new UpdateResearchGroupRequestDto("Updated Name", "Updated Description")))
                                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void updateResearchGroupShouldThrowWhenRequesterEmailNotFound() {
                when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> researchGroupService.updateResearchGroup(
                                "missing@example.com",
                                10L,
                                new UpdateResearchGroupRequestDto("Updated Name", "Updated Description")))
                                .isInstanceOf(EmailNotFoundException.class);
        }

        @Test
        void inviteResearcherByEmailShouldCreateInvitationWithRoleAndExpiration() {
                User inviter = UserTestBuilder.validUser()
                                .withEmail("admin@example.com")
                                .withFirstName("Admin")
                                .withLastName("User")
                                .build();
                setId(inviter, 1L);

                User invitedUser = UserTestBuilder.validUser().withEmail("invitee@example.com").build();
                setId(invitedUser, 2L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("NLP Group").build();
                setGroupFields(group, 10L, Instant.parse("2026-03-31T12:00:00Z"), "GROUPCODE001");

                ResearchGroupMember inviterMember = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(inviter)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ADMIN)
                                .build();

                Instant expiresAt = Instant.parse("2026-04-10T12:00:00Z");

                when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(inviter));
                when(researchGroupRepository.findById(10L)).thenReturn(Optional.of(group));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 1L))
                                .thenReturn(Optional.of(inviterMember));
                when(memberRepository.findActiveMemberEmailsByGroupId(10L)).thenReturn(List.of("admin@example.com"));
                when(invitationRepository.existsActivePendingInvitation(
                                eq(10L),
                                eq("invitee@example.com"),
                                eq(ResearchGroupInvitationStatus.PENDING),
                                any(Instant.class)))
                                .thenReturn(false);
                when(userRepository.findByEmailIgnoreCase("invitee@example.com")).thenReturn(Optional.of(invitedUser));
                when(invitationRepository.saveAndFlush(any(ResearchGroupInvitation.class))).thenAnswer(invocation -> {
                        ResearchGroupInvitation invitation = invocation.getArgument(0);
                        setInvitationFields(invitation, 100L, Instant.parse("2026-03-31T13:00:00Z"));
                        return invitation;
                });

                ResearchGroupInvitationDto result = researchGroupService.inviteResearcherByEmail(
                                "admin@example.com",
                                10L,
                                "invitee@example.com",
                                ResearchGroupMemberRole.ADMIN,
                                expiresAt);

                assertThat(result.id()).isEqualTo(100L);
                assertThat(result.role()).isEqualTo(ResearchGroupMemberRole.ADMIN);
                assertThat(result.expiresAt()).isEqualTo(expiresAt);
                verify(emailService).sendResearchGroupInvitationToExistingUser(
                                eq("invitee@example.com"),
                                eq("NLP Group"),
                                eq("Admin User"),
                                any(String.class));
        }

        @Test
        void inviteResearcherByEmailShouldThrowWhenRoleIsOwner() {
                assertThatThrownBy(() -> researchGroupService.inviteResearcherByEmail(
                                "owner@example.com",
                                10L,
                                "invitee@example.com",
                                ResearchGroupMemberRole.OWNER,
                                Instant.parse("2026-04-10T12:00:00Z")))
                                .isInstanceOf(InvalidResearchGroupInvitationRoleException.class);
        }

        @Test
        void inviteResearcherByEmailShouldThrowWhenInvitationAlreadyExists() {
                User inviter = UserTestBuilder.validUser().withEmail("admin@example.com").build();
                setId(inviter, 1L);
                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                setGroupFields(group, 10L, Instant.parse("2026-03-31T12:00:00Z"), "GROUPCODE001");
                ResearchGroupMember inviterMember = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(inviter)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ADMIN)
                                .build();

                when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(inviter));
                when(researchGroupRepository.findById(10L)).thenReturn(Optional.of(group));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 1L))
                                .thenReturn(Optional.of(inviterMember));
                when(memberRepository.findActiveMemberEmailsByGroupId(10L)).thenReturn(List.of("admin@example.com"));
                when(invitationRepository.existsActivePendingInvitation(
                                eq(10L),
                                eq("invitee@example.com"),
                                eq(ResearchGroupInvitationStatus.PENDING),
                                any(Instant.class)))
                                .thenReturn(true);

                assertThatThrownBy(() -> researchGroupService.inviteResearcherByEmail(
                                "admin@example.com",
                                10L,
                                "invitee@example.com",
                                ResearchGroupMemberRole.ANNOTATOR,
                                Instant.parse("2026-04-10T12:00:00Z")))
                                .isInstanceOf(ResearchGroupInvitationAlreadyExistsException.class);
        }

        @Test
        void inviteResearcherByEmailShouldThrowWhenEmailAlreadyBelongsToActiveMember() {
                User inviter = UserTestBuilder.validUser().withEmail("owner@example.com").build();
                setId(inviter, 1L);
                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                setGroupFields(group, 10L, Instant.parse("2026-03-31T12:00:00Z"), "GROUPCODE001");
                ResearchGroupMember inviterMember = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(inviter)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build();

                when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(inviter));
                when(researchGroupRepository.findById(10L)).thenReturn(Optional.of(group));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 1L))
                                .thenReturn(Optional.of(inviterMember));
                when(memberRepository.findActiveMemberEmailsByGroupId(10L)).thenReturn(List.of("existing@example.com"));

                assertThatThrownBy(() -> researchGroupService.inviteResearcherByEmail(
                                "owner@example.com",
                                10L,
                                "existing@example.com",
                                ResearchGroupMemberRole.ANNOTATOR,
                                Instant.parse("2026-04-10T12:00:00Z")))
                                .isInstanceOf(ResearchGroupMemberAlreadyExistsException.class);
        }

        @Test
        void findMyPendingInvitationsShouldReturnPendingInvitationsForUserEmail() {
                User invitedUser = UserTestBuilder.validUser().withEmail("invited@example.com").build();
                setId(invitedUser, 15L);

                ResearchGroupInvitationDto dto = new ResearchGroupInvitationDto(
                                1L,
                                10L,
                                "NLP Group",
                                "invited@example.com",
                                "Admin User",
                                ResearchGroupMemberRole.ANNOTATOR,
                                ResearchGroupInvitationStatus.PENDING,
                                Instant.parse("2026-03-31T12:00:00Z"),
                                Instant.parse("2026-04-10T12:00:00Z"));

                when(userRepository.findByEmailIgnoreCase("invited@example.com")).thenReturn(Optional.of(invitedUser));
                when(invitationRepository.findPendingInvitationsByInvitedEmail(eq("invited@example.com"),
                                any(Instant.class)))
                                .thenReturn(List.of(dto));

                List<ResearchGroupInvitationDto> invitations = researchGroupService
                                .findMyPendingInvitations("invited@example.com");

                assertThat(invitations).hasSize(1);
                assertThat(invitations.get(0).role()).isEqualTo(ResearchGroupMemberRole.ANNOTATOR);
        }

        @Test
        void joinResearchGroupByCodeShouldCreateAnnotatorMembership() {
                User user = UserTestBuilder.validUser().withEmail("joiner@example.com").build();
                setId(user, 21L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("Joinable Group").build();
                setGroupFields(group, 44L, Instant.parse("2026-03-31T12:00:00Z"), "JOINCODE12345");

                when(userRepository.findByEmailIgnoreCase("joiner@example.com")).thenReturn(Optional.of(user));
                when(researchGroupRepository.findByInvitationCodeIgnoreCase("JOINCODE12345"))
                                .thenReturn(Optional.of(group));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(44L, 21L)).thenReturn(Optional.empty());
                when(memberRepository.findActiveMemberEmailsByGroupId(44L))
                                .thenReturn(List.of("owner@example.com", "joiner@example.com"));

                ResearchGroupSummaryDto result = researchGroupService.joinResearchGroupByCode("joiner@example.com",
                                "JOINCODE12345");

                assertThat(result.id()).isEqualTo(44L);
                assertThat(result.role()).isEqualTo(ResearchGroupMemberRole.ANNOTATOR);
                assertThat(result.memberCount()).isEqualTo(2L);
        }

        @Test
        void joinResearchGroupByCodeShouldResolvePendingInvitationsForUserAndGroup() {
                User user = UserTestBuilder.validUser().withEmail("joiner@example.com").build();
                setId(user, 21L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("Joinable Group").build();
                setGroupFields(group, 44L, Instant.parse("2026-03-31T12:00:00Z"), "JOINCODE12345");

                ResearchGroupInvitation pendingInvitation = new ResearchGroupInvitation();
                pendingInvitation.setStatus(ResearchGroupInvitationStatus.PENDING);

                when(userRepository.findByEmailIgnoreCase("joiner@example.com")).thenReturn(Optional.of(user));
                when(researchGroupRepository.findByInvitationCodeIgnoreCase("JOINCODE12345"))
                                .thenReturn(Optional.of(group));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(44L, 21L)).thenReturn(Optional.empty());
                when(invitationRepository.findActivePendingInvitationsByGroupIdAndInvitedEmail(
                                eq(44L),
                                eq("joiner@example.com"),
                                any(Instant.class)))
                                .thenReturn(List.of(pendingInvitation));
                when(memberRepository.findActiveMemberEmailsByGroupId(44L))
                                .thenReturn(List.of("owner@example.com", "joiner@example.com"));

                researchGroupService.joinResearchGroupByCode("joiner@example.com", "JOINCODE12345");

                ArgumentCaptor<List<ResearchGroupInvitation>> invitationsCaptor = ArgumentCaptor.forClass(List.class);
                verify(invitationRepository).saveAll(invitationsCaptor.capture());

                List<ResearchGroupInvitation> savedInvitations = invitationsCaptor.getValue();
                assertThat(savedInvitations).hasSize(1);
                assertThat(savedInvitations.get(0).getStatus()).isEqualTo(ResearchGroupInvitationStatus.ACCEPTED);
                assertThat(savedInvitations.get(0).getInvitedUser()).isEqualTo(user);
        }

        @Test
        void joinResearchGroupByCodeShouldThrowWhenCodeDoesNotExist() {
                User user = UserTestBuilder.validUser().withEmail("joiner@example.com").build();
                setId(user, 21L);

                when(userRepository.findByEmailIgnoreCase("joiner@example.com")).thenReturn(Optional.of(user));
                when(researchGroupRepository.findByInvitationCodeIgnoreCase("UNKNOWNCODE"))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(
                                () -> researchGroupService.joinResearchGroupByCode("joiner@example.com", "UNKNOWNCODE"))
                                .isInstanceOf(ResearchGroupInvitationCodeNotFoundException.class);
        }

        @Test
        void joinResearchGroupByCodeShouldThrowWhenUserIsAlreadyMember() {
                User user = UserTestBuilder.validUser().withEmail("member@example.com").build();
                setId(user, 31L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                setGroupFields(group, 77L, Instant.parse("2026-03-31T12:00:00Z"), "EXISTCODE123");

                ResearchGroupMember existingMember = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(user)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ADMIN)
                                .build();

                when(userRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(user));
                when(researchGroupRepository.findByInvitationCodeIgnoreCase("EXISTCODE123"))
                                .thenReturn(Optional.of(group));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(77L, 31L))
                                .thenReturn(Optional.of(existingMember));

                assertThatThrownBy(() -> researchGroupService.joinResearchGroupByCode("member@example.com",
                                "EXISTCODE123"))
                                .isInstanceOf(ResearchGroupMemberAlreadyExistsException.class);
        }

        @Test
        void updateMemberRoleShouldSucceedWhenRequesterIsOwner() {
                User owner = UserTestBuilder.validUser().withEmail("owner@example.com").build();
                setId(owner, 1L);

                User target = UserTestBuilder.validUser().withEmail("member@example.com").build();
                setId(target, 2L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                setGroupFields(group, 10L, Instant.parse("2026-03-31T12:00:00Z"), "GROUPCODE001");

                ResearchGroupMember ownerMembership = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build();

                ResearchGroupMember targetMembership = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(target)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                                .build();

                when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
                when(researchGroupRepository.existsById(10L)).thenReturn(true);
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 1L))
                                .thenReturn(Optional.of(ownerMembership));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 2L))
                                .thenReturn(Optional.of(targetMembership));
                when(memberRepository.save(any(ResearchGroupMember.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                ResearchGroupMemberDto result = researchGroupService.updateMemberRole(
                                "owner@example.com", 10L, 2L, ResearchGroupMemberRole.ADMIN);

                assertThat(result.userId()).isEqualTo(2L);
                assertThat(result.role()).isEqualTo(ResearchGroupMemberRole.ADMIN);
        }

        @Test
        void updateMemberRoleShouldThrowWhenRequesterIsNotOwner() {
                User admin = UserTestBuilder.validUser().withEmail("admin@example.com").build();
                setId(admin, 1L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                setGroupFields(group, 10L, Instant.parse("2026-03-31T12:00:00Z"), "GROUPCODE001");

                ResearchGroupMember adminMembership = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(admin)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ADMIN)
                                .build();

                when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
                when(researchGroupRepository.existsById(10L)).thenReturn(true);
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 1L))
                                .thenReturn(Optional.of(adminMembership));

                assertThatThrownBy(() -> researchGroupService.updateMemberRole(
                                "admin@example.com", 10L, 2L, ResearchGroupMemberRole.ANNOTATOR))
                                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void updateMemberRoleShouldThrowWhenTargetMemberIsMissing() {
                User owner = UserTestBuilder.validUser().withEmail("owner@example.com").build();
                setId(owner, 1L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                setGroupFields(group, 10L, Instant.parse("2026-03-31T12:00:00Z"), "GROUPCODE001");

                ResearchGroupMember ownerMembership = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build();

                when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
                when(researchGroupRepository.existsById(10L)).thenReturn(true);
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 1L))
                                .thenReturn(Optional.of(ownerMembership));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 99L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> researchGroupService.updateMemberRole(
                                "owner@example.com", 10L, 99L, ResearchGroupMemberRole.ADMIN))
                                .isInstanceOf(ResearchGroupMemberNotFoundException.class);
        }

        @Test
        void updateMemberRoleShouldThrowWhenTargetMemberIsOwner() {
                User ownerRequester = UserTestBuilder.validUser().withEmail("owner.requester@example.com").build();
                setId(ownerRequester, 1L);

                User ownerTarget = UserTestBuilder.validUser().withEmail("owner.target@example.com").build();
                setId(ownerTarget, 2L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                setGroupFields(group, 10L, Instant.parse("2026-03-31T12:00:00Z"), "GROUPCODE001");

                ResearchGroupMember requesterMembership = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(ownerRequester)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build();

                ResearchGroupMember ownerTargetMembership = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(ownerTarget)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build();

                when(userRepository.findByEmailIgnoreCase("owner.requester@example.com"))
                                .thenReturn(Optional.of(ownerRequester));
                when(researchGroupRepository.existsById(10L)).thenReturn(true);
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 1L))
                                .thenReturn(Optional.of(requesterMembership));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 2L))
                                .thenReturn(Optional.of(ownerTargetMembership));

                assertThatThrownBy(() -> researchGroupService.updateMemberRole(
                                "owner.requester@example.com", 10L, 2L, ResearchGroupMemberRole.ADMIN))
                                .isInstanceOf(InvalidResearchGroupMemberRoleException.class);
        }

        @Test
        void updateMemberRoleShouldThrowWhenGroupDoesNotExist() {
                User owner = UserTestBuilder.validUser().withEmail("owner@example.com").build();
                setId(owner, 1L);

                when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
                when(researchGroupRepository.existsById(999L)).thenReturn(false);

                assertThatThrownBy(() -> researchGroupService.updateMemberRole(
                                "owner@example.com", 999L, 2L, ResearchGroupMemberRole.ADMIN))
                                .isInstanceOf(ResearchGroupNotFoundException.class);
        }

        @Test
        void removeMemberShouldSoftDeleteWhenRequesterIsOwner() {
                User owner = UserTestBuilder.validUser().withEmail("owner@example.com").build();
                setId(owner, 1L);

                User target = UserTestBuilder.validUser().withEmail("member@example.com").build();
                setId(target, 2L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                setGroupFields(group, 10L, Instant.parse("2026-03-31T12:00:00Z"), "GROUPCODE001");

                ResearchGroupMember ownerMembership = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build();

                ResearchGroupMember targetMembership = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(target)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ANNOTATOR)
                                .build();

                when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
                when(researchGroupRepository.existsById(10L)).thenReturn(true);
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 1L))
                                .thenReturn(Optional.of(ownerMembership));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 2L))
                                .thenReturn(Optional.of(targetMembership));

                researchGroupService.removeMember("owner@example.com", 10L, 2L);

                assertThat(targetMembership.getDeletedAt()).isNotNull();
                verify(memberRepository).save(targetMembership);
        }

        @Test
        void removeMemberShouldThrowWhenRequesterIsNotOwner() {
                User admin = UserTestBuilder.validUser().withEmail("admin@example.com").build();
                setId(admin, 1L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                setGroupFields(group, 10L, Instant.parse("2026-03-31T12:00:00Z"), "GROUPCODE001");

                ResearchGroupMember adminMembership = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(admin)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.ADMIN)
                                .build();

                when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
                when(researchGroupRepository.existsById(10L)).thenReturn(true);
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 1L))
                                .thenReturn(Optional.of(adminMembership));

                assertThatThrownBy(() -> researchGroupService.removeMember("admin@example.com", 10L, 2L))
                                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void removeMemberShouldThrowWhenTargetMemberIsMissing() {
                User owner = UserTestBuilder.validUser().withEmail("owner@example.com").build();
                setId(owner, 1L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                setGroupFields(group, 10L, Instant.parse("2026-03-31T12:00:00Z"), "GROUPCODE001");

                ResearchGroupMember ownerMembership = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(owner)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build();

                when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
                when(researchGroupRepository.existsById(10L)).thenReturn(true);
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 1L))
                                .thenReturn(Optional.of(ownerMembership));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 999L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> researchGroupService.removeMember("owner@example.com", 10L, 999L))
                                .isInstanceOf(ResearchGroupMemberNotFoundException.class);
        }

        @Test
        void removeMemberShouldThrowWhenTargetMemberIsOwner() {
                User ownerRequester = UserTestBuilder.validUser().withEmail("owner.requester@example.com").build();
                setId(ownerRequester, 1L);

                User ownerTarget = UserTestBuilder.validUser().withEmail("owner.target@example.com").build();
                setId(ownerTarget, 2L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().build();
                setGroupFields(group, 10L, Instant.parse("2026-03-31T12:00:00Z"), "GROUPCODE001");

                ResearchGroupMember requesterMembership = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(ownerRequester)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build();

                ResearchGroupMember ownerTargetMembership = ResearchGroupMemberTestBuilder.validMember()
                                .withUser(ownerTarget)
                                .withResearchGroup(group)
                                .withRole(ResearchGroupMemberRole.OWNER)
                                .build();

                when(userRepository.findByEmailIgnoreCase("owner.requester@example.com"))
                                .thenReturn(Optional.of(ownerRequester));
                when(researchGroupRepository.existsById(10L)).thenReturn(true);
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 1L))
                                .thenReturn(Optional.of(requesterMembership));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 2L))
                                .thenReturn(Optional.of(ownerTargetMembership));

                assertThatThrownBy(() -> researchGroupService.removeMember(
                                "owner.requester@example.com", 10L, 2L))
                                .isInstanceOf(InvalidResearchGroupMemberRoleException.class);
        }

        @Test
        void manageMembersShouldRejectOwnerRoleOperations() {
                assertThatThrownBy(() -> researchGroupService.updateMemberRole(
                                "owner@example.com", 10L, 2L, ResearchGroupMemberRole.OWNER))
                                .isInstanceOf(InvalidResearchGroupMemberRoleException.class);
        }

        @Test
        void acceptMyInvitationShouldCreateMembershipAndMarkInvitationAsAccepted() {
                User user = UserTestBuilder.validUser().withEmail("invitee@example.com").build();
                setId(user, 50L);

                User inviter = UserTestBuilder.validUser().withEmail("owner@example.com").build();
                setId(inviter, 1L);

                ResearchGroup group = ResearchGroupTestBuilder.validGroup().withName("NLP Group").build();
                setGroupFields(group, 10L, Instant.parse("2026-03-31T12:00:00Z"), "GROUPCODE001");

                ResearchGroupInvitation invitation = new ResearchGroupInvitation();
                invitation.setResearchGroup(group);
                invitation.setInviterUser(inviter);
                invitation.setInvitedEmail("invitee@example.com");
                invitation.setRole(ResearchGroupMemberRole.ADMIN);
                invitation.setStatus(ResearchGroupInvitationStatus.PENDING);
                invitation.setExpiresAt(Instant.now().plusSeconds(86400));

                when(userRepository.findByEmailIgnoreCase("invitee@example.com")).thenReturn(Optional.of(user));
                when(invitationRepository.findActivePendingInvitationByIdAndInvitedEmail(
                                eq(100L),
                                eq("invitee@example.com"),
                                any(Instant.class)))
                                .thenReturn(Optional.of(invitation));
                when(memberRepository.findActiveMemberByGroupIdAndUserId(10L, 50L)).thenReturn(Optional.empty());
                when(memberRepository.findActiveMemberEmailsByGroupId(10L))
                                .thenReturn(List.of("owner@example.com", "invitee@example.com"));

                ResearchGroupSummaryDto result = researchGroupService.acceptMyInvitation("invitee@example.com", 100L);

                assertThat(result.id()).isEqualTo(10L);
                assertThat(result.role()).isEqualTo(ResearchGroupMemberRole.ADMIN);
                assertThat(result.memberCount()).isEqualTo(2L);
                assertThat(invitation.getStatus()).isEqualTo(ResearchGroupInvitationStatus.ACCEPTED);
                assertThat(invitation.getInvitedUser()).isEqualTo(user);
                verify(memberRepository).save(any(ResearchGroupMember.class));
                verify(invitationRepository).save(invitation);
        }

        @Test
        void declineMyInvitationShouldMarkInvitationAsDeclined() {
                User user = UserTestBuilder.validUser().withEmail("invitee@example.com").build();
                setId(user, 50L);

                ResearchGroupInvitation invitation = new ResearchGroupInvitation();
                invitation.setInvitedEmail("invitee@example.com");
                invitation.setStatus(ResearchGroupInvitationStatus.PENDING);
                invitation.setExpiresAt(Instant.now().plusSeconds(86400));

                when(userRepository.findByEmailIgnoreCase("invitee@example.com")).thenReturn(Optional.of(user));
                when(invitationRepository.findActivePendingInvitationByIdAndInvitedEmail(
                                eq(101L),
                                eq("invitee@example.com"),
                                any(Instant.class)))
                                .thenReturn(Optional.of(invitation));

                researchGroupService.declineMyInvitation("invitee@example.com", 101L);

                assertThat(invitation.getStatus()).isEqualTo(ResearchGroupInvitationStatus.DECLINED);
                verify(invitationRepository).save(invitation);
        }

        @Test
        void acceptMyInvitationShouldThrowWhenInvitationIsNotFound() {
                User user = UserTestBuilder.validUser().withEmail("invitee@example.com").build();
                setId(user, 50L);

                when(userRepository.findByEmailIgnoreCase("invitee@example.com")).thenReturn(Optional.of(user));
                when(invitationRepository.findActivePendingInvitationByIdAndInvitedEmail(
                                eq(999L),
                                eq("invitee@example.com"),
                                any(Instant.class)))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> researchGroupService.acceptMyInvitation("invitee@example.com", 999L))
                                .isInstanceOf(ResearchGroupInvitationNotFoundException.class);
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

        private void setGroupFields(ResearchGroup group, Long id, Instant createdAt, String invitationCode) {
                try {
                        java.lang.reflect.Field idField = ResearchGroup.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(group, id);

                        java.lang.reflect.Field createdAtField = ResearchGroup.class.getDeclaredField("createdAt");
                        createdAtField.setAccessible(true);
                        createdAtField.set(group, createdAt);

                        java.lang.reflect.Field codeField = ResearchGroup.class.getDeclaredField("invitationCode");
                        codeField.setAccessible(true);
                        codeField.set(group, invitationCode);
                } catch (ReflectiveOperationException ex) {
                        throw new RuntimeException(ex);
                }
        }

        private void setInvitationFields(ResearchGroupInvitation invitation, Long id, Instant createdAt) {
                try {
                        java.lang.reflect.Field idField = ResearchGroupInvitation.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(invitation, id);

                        java.lang.reflect.Field createdAtField = ResearchGroupInvitation.class
                                        .getDeclaredField("createdAt");
                        createdAtField.setAccessible(true);
                        createdAtField.set(invitation, createdAt);
                } catch (ReflectiveOperationException ex) {
                        throw new RuntimeException(ex);
                }
        }
}
