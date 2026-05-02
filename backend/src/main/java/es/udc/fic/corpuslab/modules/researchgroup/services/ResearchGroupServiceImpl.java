package es.udc.fic.corpuslab.modules.researchgroup.services;

import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.notification.services.EmailService;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.api.ProjectApiService;
import es.udc.fic.corpuslab.modules.project.services.ProjectParticipantService;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.CreateResearchGroupRequestDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupInvitationDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupDetailDto;
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
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupInvitationEmailDeliveryException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupInvitationNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupMemberAlreadyExistsException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupMemberNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupInvitationRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;
import jakarta.persistence.EntityManager;

@Service
public class ResearchGroupServiceImpl implements ResearchGroupService {

    private static final int MAX_TOKEN_GENERATION_ATTEMPTS = 10;

    private final AuthApiService authApiService;
    private final ResearchGroupRepository researchGroupRepository;
    private final ResearchGroupMemberRepository memberRepository;
    private final ResearchGroupInvitationRepository invitationRepository;
    private final ProjectApiService projectApiService;
    private final ProjectParticipantService projectParticipantService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final EntityManager entityManager;
    private final String frontendBaseUrl;

    public ResearchGroupServiceImpl(
            AuthApiService authApiService,
            ResearchGroupRepository researchGroupRepository,
            ResearchGroupMemberRepository memberRepository,
            ResearchGroupInvitationRepository invitationRepository,
            ProjectApiService projectApiService,
            ProjectParticipantService projectParticipantService,
            NotificationService notificationService,
            EmailService emailService,
            EntityManager entityManager,
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.authApiService = authApiService;
        this.researchGroupRepository = researchGroupRepository;
        this.memberRepository = memberRepository;
        this.invitationRepository = invitationRepository;
        this.projectApiService = projectApiService;
        this.projectParticipantService = projectParticipantService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.entityManager = entityManager;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResearchGroupSummaryDto> findMyResearchGroups(String authenticatedEmail) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);
        return memberRepository.findGroupSummariesByUserId(userInfo.userId());
    }

    @Override
    @Transactional
    public ResearchGroupSummaryDto createResearchGroup(String authenticatedEmail,
            CreateResearchGroupRequestDto request) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        ResearchGroup group = new ResearchGroup();
        group.setName(request.name().trim());
        group.setDescription(
                request.description() != null && !request.description().isBlank()
                        ? request.description().trim()
                        : null);
        group = researchGroupRepository.save(group);

        ResearchGroupMember ownerMember = new ResearchGroupMember();
        ownerMember.setUser(getUserReference(userInfo.userId()));
        ownerMember.setResearchGroup(group);
        ownerMember.setRole(ResearchGroupMemberRole.OWNER);
        memberRepository.save(ownerMember);

        return new ResearchGroupSummaryDto(
                group.getId(),
                group.getName(),
                group.getDescription(),
                ResearchGroupMemberRole.OWNER,
                1L,
                group.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public ResearchGroupDetailDto getResearchGroupDetail(String authenticatedEmail, Long groupId) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        ResearchGroup group = researchGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResearchGroupNotFoundException(groupId));

        List<ResearchGroupMemberDto> members = memberRepository.findMembersByGroupId(groupId);
        boolean isMember = members.stream().anyMatch(m -> m.userId().equals(userInfo.userId()));

        if (!isMember) {
            throw new AccessDeniedException("User is not a member of this research group");
        }

        long activeProjects = projectApiService.countProjectsByResearchGroupId(groupId);

        return new ResearchGroupDetailDto(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getInvitationCode(),
                members.size(),
                activeProjects,
                group.getCreatedAt(),
                members);
    }

    @Override
    @Transactional
    public ResearchGroupDetailDto updateResearchGroup(
            String authenticatedEmail,
            Long groupId,
            UpdateResearchGroupRequestDto request) {
        UserInfo requesterInfo = authApiService.findUserByEmail(authenticatedEmail);

        ResearchGroup group = researchGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResearchGroupNotFoundException(groupId));

        ResearchGroupMember requesterMembership = memberRepository
                .findActiveMemberByGroupIdAndUserId(groupId, requesterInfo.userId())
                .orElseThrow(() -> new AccessDeniedException(
                        "User is not a member of this research group"));

        if (requesterMembership.getRole() != ResearchGroupMemberRole.OWNER) {
            throw new AccessDeniedException("Only owners can edit this research group");
        }

        group.setName(request.name().trim());
        group.setDescription(
                request.description() != null && !request.description().isBlank()
                        ? request.description().trim()
                        : null);
        researchGroupRepository.save(group);

        List<ResearchGroupMemberDto> members = memberRepository.findMembersByGroupId(groupId);
        long activeProjects = projectApiService.countProjectsByResearchGroupId(groupId);

        return new ResearchGroupDetailDto(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getInvitationCode(),
                members.size(),
                activeProjects,
                group.getCreatedAt(),
                members);
    }

    @Override
    @Transactional
    public void deleteResearchGroup(String authenticatedEmail, Long groupId) {
        UserInfo requesterInfo = authApiService.findUserByEmail(authenticatedEmail);
        validateOwnerPermissions(groupId, requesterInfo.userId());

        ResearchGroup group = researchGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResearchGroupNotFoundException(groupId));

        projectApiService.deleteAllProjectsByResearchGroupId(groupId);

        notificationService.deleteNotificationsByResearchGroupId(groupId);
        invitationRepository.deleteByResearchGroupId(groupId);
        memberRepository.deleteByResearchGroupId(groupId);
        researchGroupRepository.delete(group);
    }

    @Override
    @Transactional
    public ResearchGroupInvitationDto inviteResearcherByEmail(
            String authenticatedEmail,
            Long groupId,
            String invitedEmail,
            ResearchGroupMemberRole role,
            Instant expiresAt) {
        UserInfo inviterInfo = authApiService.findUserByEmail(authenticatedEmail);
        String normalizedInvitedEmail = invitedEmail.trim().toLowerCase(Locale.ROOT);

        if (role == ResearchGroupMemberRole.OWNER) {
            throw new InvalidResearchGroupInvitationRoleException(role);
        }

        ResearchGroup group = researchGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResearchGroupNotFoundException(groupId));

        ResearchGroupMember inviterMembership = memberRepository
                .findActiveMemberByGroupIdAndUserId(groupId, inviterInfo.userId())
                .orElseThrow(() -> new AccessDeniedException(
                        "User is not a member of this research group"));

        if (inviterMembership.getRole() != ResearchGroupMemberRole.OWNER
                && inviterMembership.getRole() != ResearchGroupMemberRole.ADMIN) {
            throw new AccessDeniedException("Only owners or admins can invite researchers");
        }

        List<String> activeMemberEmails = memberRepository.findActiveMemberEmailsByGroupId(groupId);
        if (activeMemberEmails.stream().anyMatch(email -> email.equalsIgnoreCase(normalizedInvitedEmail))) {
            throw new ResearchGroupMemberAlreadyExistsException(groupId, normalizedInvitedEmail);
        }

        boolean alreadyPending = invitationRepository.existsActivePendingInvitation(
                groupId,
                normalizedInvitedEmail,
                ResearchGroupInvitationStatus.PENDING,
                Instant.now());
        if (alreadyPending) {
            throw new ResearchGroupInvitationAlreadyExistsException(groupId, normalizedInvitedEmail);
        }

        // Check if the invited email belongs to an existing user
        UserInfo invitedUserInfo = authApiService.findUserByEmailOptional(normalizedInvitedEmail).orElse(null);

        ResearchGroupInvitation invitation = new ResearchGroupInvitation();
        invitation.setResearchGroup(group);
        invitation.setInviterUser(getUserReference(inviterInfo.userId()));
        invitation.setInvitedUser(invitedUserInfo != null ? getUserReference(invitedUserInfo.userId()) : null);
        invitation.setInvitedEmail(normalizedInvitedEmail);
        invitation.setRole(role);
        invitation.setStatus(ResearchGroupInvitationStatus.PENDING);
        invitation.setExpiresAt(expiresAt);
        invitation = saveInvitationWithUniqueToken(invitation);

        if (invitedUserInfo != null) {
            notificationService.createResearchGroupInvitationReceivedNotification(
                    invitedUserInfo.userId(),
                    inviterInfo.userId(),
                    group.getId(),
                    group.getName(),
                    invitation.getId());
        }

        String inviterFullName = inviterInfo.fullName();
        String invitationUrl = frontendBaseUrl + "/home/research-groups";
        String signupUrl = frontendBaseUrl + "/auth/signup?invitationToken=" + invitation.getToken();

        try {
            if (invitedUserInfo != null) {
                emailService.sendResearchGroupInvitationToExistingUser(
                        normalizedInvitedEmail,
                        group.getName(),
                        inviterFullName,
                        invitationUrl);
            } else {
                emailService.sendResearchGroupInvitationToNewUser(
                        normalizedInvitedEmail,
                        group.getName(),
                        inviterFullName,
                        signupUrl);
            }
        } catch (RuntimeException ex) {
            throw new ResearchGroupInvitationEmailDeliveryException(ex);
        }

        return new ResearchGroupInvitationDto(
                invitation.getId(),
                group.getId(),
                group.getName(),
                invitation.getInvitedEmail(),
                inviterFullName,
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getCreatedAt(),
                invitation.getExpiresAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResearchGroupInvitationDto> findMyPendingInvitations(String authenticatedEmail) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);
        return invitationRepository.findPendingInvitationsByInvitedEmail(userInfo.email(), Instant.now());
    }

    @Override
    @Transactional
    public ResearchGroupSummaryDto acceptMyInvitation(String authenticatedEmail, Long invitationId) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        ResearchGroupInvitation invitation = findActivePendingInvitationForUser(invitationId, userInfo.email());
        ResearchGroup group = invitation.getResearchGroup();

        if (memberRepository.findActiveMemberByGroupIdAndUserId(group.getId(), userInfo.userId()).isPresent()) {
            throw new ResearchGroupMemberAlreadyExistsException(group.getId(), userInfo.email());
        }

        ResearchGroupMember member = new ResearchGroupMember();
        member.setResearchGroup(group);
        member.setUser(getUserReference(userInfo.userId()));
        member.setRole(invitation.getRole());
        memberRepository.save(member);

        invitation.setInvitedUser(getUserReference(userInfo.userId()));
        invitation.setStatus(ResearchGroupInvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        User inviterUser = invitation.getInviterUser();
        if (inviterUser != null && !inviterUser.getId().equals(userInfo.userId())) {
            notificationService.createResearchGroupInvitationAcceptedNotification(
                    inviterUser.getId(),
                    userInfo.userId(),
                    group.getId(),
                    group.getName());
        }

        long memberCount = memberRepository.findActiveMemberEmailsByGroupId(group.getId()).size();

        return new ResearchGroupSummaryDto(
                group.getId(),
                group.getName(),
                group.getDescription(),
                invitation.getRole(),
                memberCount,
                group.getCreatedAt());
    }

    @Override
    @Transactional
    public void declineMyInvitation(String authenticatedEmail, Long invitationId) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        ResearchGroupInvitation invitation = findActivePendingInvitationForUser(invitationId, userInfo.email());
        invitation.setStatus(ResearchGroupInvitationStatus.DECLINED);
        invitationRepository.save(invitation);
    }

    @Override
    @Transactional
    public ResearchGroupMemberDto updateMemberRole(
            String authenticatedEmail,
            Long groupId,
            Long memberUserId,
            ResearchGroupMemberRole role) {
        if (role == ResearchGroupMemberRole.OWNER) {
            throw new InvalidResearchGroupMemberRoleException(role);
        }

        UserInfo requesterInfo = authApiService.findUserByEmail(authenticatedEmail);
        validateOwnerPermissions(groupId, requesterInfo.userId());

        ResearchGroupMember targetMember = memberRepository
                .findActiveMemberByGroupIdAndUserId(groupId, memberUserId)
                .orElseThrow(() -> new ResearchGroupMemberNotFoundException(groupId, memberUserId));

        if (targetMember.getRole() == ResearchGroupMemberRole.OWNER) {
            throw new InvalidResearchGroupMemberRoleException(targetMember.getRole());
        }

        targetMember.setRole(role);
        ResearchGroupMember saved = memberRepository.save(targetMember);

        // Use the member repository's JPQL query which already counts project participations
        List<ResearchGroupMemberDto> members = memberRepository.findMembersByGroupId(groupId);
        ResearchGroupMemberDto updatedMember = members.stream()
                .filter(m -> m.userId().equals(saved.getUser().getId()))
                .findFirst()
                .orElse(new ResearchGroupMemberDto(
                        saved.getUser().getId(),
                        saved.getUser().getFirstName(),
                        saved.getUser().getLastName(),
                        saved.getUser().getEmail(),
                        saved.getRole(),
                        0));

        return updatedMember;
    }

    @Override
    @Transactional
    public void removeMember(String authenticatedEmail, Long groupId, Long memberUserId) {
        UserInfo requesterInfo = authApiService.findUserByEmail(authenticatedEmail);
        validateOwnerPermissions(groupId, requesterInfo.userId());

        ResearchGroupMember targetMember = memberRepository
                .findActiveMemberByGroupIdAndUserId(groupId, memberUserId)
                .orElseThrow(() -> new ResearchGroupMemberNotFoundException(groupId, memberUserId));

        if (targetMember.getRole() == ResearchGroupMemberRole.OWNER) {
            throw new InvalidResearchGroupMemberRoleException(targetMember.getRole());
        }

        targetMember.setDeletedAt(Instant.now());
        memberRepository.save(targetMember);

        projectParticipantService.removeParticipantFromAllGroupProjects(groupId, memberUserId);
    }

    @Override
    @Transactional
    public ResearchGroupSummaryDto joinResearchGroupByCode(String authenticatedEmail, String invitationCode) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);
        String normalizedCode = invitationCode.trim().toUpperCase(Locale.ROOT);

        ResearchGroup group = researchGroupRepository.findByInvitationCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new ResearchGroupInvitationCodeNotFoundException(normalizedCode));

        if (memberRepository.findActiveMemberByGroupIdAndUserId(group.getId(), userInfo.userId()).isPresent()) {
            throw new ResearchGroupMemberAlreadyExistsException(group.getId(), userInfo.email());
        }

        ResearchGroupMember member = new ResearchGroupMember();
        member.setResearchGroup(group);
        member.setUser(getUserReference(userInfo.userId()));
        member.setRole(ResearchGroupMemberRole.ANNOTATOR);
        memberRepository.save(member);

        resolvePendingInvitationsAfterJoinByCode(group.getId(), userInfo.email(), getUserReference(userInfo.userId()));

        long memberCount = memberRepository.findActiveMemberEmailsByGroupId(group.getId()).size();

        return new ResearchGroupSummaryDto(
                group.getId(),
                group.getName(),
                group.getDescription(),
                ResearchGroupMemberRole.ANNOTATOR,
                memberCount,
                group.getCreatedAt());
    }

    private ResearchGroupInvitation saveInvitationWithUniqueToken(ResearchGroupInvitation invitation) {
        for (int attempt = 1; attempt <= MAX_TOKEN_GENERATION_ATTEMPTS; attempt++) {
            String token = UUID.randomUUID().toString();
            if (invitationRepository.existsByToken(token)) {
                continue;
            }
            invitation.setToken(token);
            try {
                return invitationRepository.saveAndFlush(invitation);
            } catch (DataIntegrityViolationException ex) {
                if (invitationRepository.existsByToken(token)) {
                    continue;
                }
                throw ex;
            }
        }
        throw new IllegalStateException("Unable to generate a unique invitation token");
    }

    private ResearchGroupInvitation findActivePendingInvitationForUser(Long invitationId, String normalizedEmail) {
        return invitationRepository
                .findActivePendingInvitationByIdAndInvitedEmail(invitationId, normalizedEmail, Instant.now())
                .orElseThrow(() -> new ResearchGroupInvitationNotFoundException(invitationId));
    }

    private void resolvePendingInvitationsAfterJoinByCode(Long groupId, String normalizedEmail, User userRef) {
        List<ResearchGroupInvitation> pendingInvitations = invitationRepository
                .findActivePendingInvitationsByGroupIdAndInvitedEmail(groupId, normalizedEmail, Instant.now());

        if (pendingInvitations.isEmpty()) {
            return;
        }

        pendingInvitations.forEach(invitation -> {
            invitation.setInvitedUser(userRef);
            invitation.setStatus(ResearchGroupInvitationStatus.ACCEPTED);
        });

        invitationRepository.saveAll(pendingInvitations);
    }

    private void validateOwnerPermissions(Long groupId, Long requesterUserId) {
        if (!researchGroupRepository.existsById(groupId)) {
            throw new ResearchGroupNotFoundException(groupId);
        }

        ResearchGroupMember requesterMembership = memberRepository
                .findActiveMemberByGroupIdAndUserId(groupId, requesterUserId)
                .orElseThrow(() -> new AccessDeniedException(
                        "User is not a member of this research group"));

        if (requesterMembership.getRole() != ResearchGroupMemberRole.OWNER) {
            throw new AccessDeniedException("Only owners can manage researchers");
        }
    }

    /**
     * Creates a JPA proxy reference for User without loading the entity.
     */
    private User getUserReference(Long userId) {
        return entityManager.getReference(User.class, userId);
    }
}
