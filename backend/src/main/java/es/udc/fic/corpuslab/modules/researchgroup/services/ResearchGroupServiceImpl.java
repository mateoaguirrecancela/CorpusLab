package es.udc.fic.corpuslab.modules.researchgroup.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.common.utils.EmailNormalizer;
import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.notification.services.EmailService;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.api.ProjectApiService;
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

@Service
public class ResearchGroupServiceImpl implements ResearchGroupService {

    private final AuthApiService authApiService;
    private final ResearchGroupRepository researchGroupRepository;
    private final ResearchGroupMemberRepository memberRepository;
    private final ResearchGroupInvitationRepository invitationRepository;
    private final ProjectApiService projectApiService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final ResearchGroupEntityReferenceService entityReferenceService;
    private final String frontendBaseUrl;
    private final long invitationExpirationDays;

    public ResearchGroupServiceImpl(
            AuthApiService authApiService,
            ResearchGroupRepository researchGroupRepository,
            ResearchGroupMemberRepository memberRepository,
            ResearchGroupInvitationRepository invitationRepository,
            ProjectApiService projectApiService,
            NotificationService notificationService,
            EmailService emailService,
            ResearchGroupEntityReferenceService entityReferenceService,
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl,
            @Value("${app.research-group.invitation-expiration-days:7}") long invitationExpirationDays) {
        this.authApiService = authApiService;
        this.researchGroupRepository = researchGroupRepository;
        this.memberRepository = memberRepository;
        this.invitationRepository = invitationRepository;
        this.projectApiService = projectApiService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.entityReferenceService = entityReferenceService;
        this.frontendBaseUrl = frontendBaseUrl;
        this.invitationExpirationDays = Math.max(1L, invitationExpirationDays);
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
        group.setDescription(normalizeNullableText(request.description()));
        group = researchGroupRepository.save(group);

        ResearchGroupMember ownerMember = new ResearchGroupMember();
        entityReferenceService.attachUser(ownerMember, userInfo.userId());
        ownerMember.setResearchGroup(group);
        ownerMember.setRole(ResearchGroupMemberRole.OWNER);
        memberRepository.save(ownerMember);

        return toSummaryDto(group, ResearchGroupMemberRole.OWNER, 1L);
    }

    @Override
    @Transactional(readOnly = true)
    public ResearchGroupDetailDto getResearchGroupDetail(String authenticatedEmail, Long groupId) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        ResearchGroup group = researchGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResearchGroupNotFoundException(groupId));

        ResearchGroupMember currentMember = memberRepository
                .findActiveMemberByGroupIdAndUserId(groupId, userInfo.userId())
                .orElseThrow(() -> new AccessDeniedException(
                        "User is not a member of this research group"));

        return toDetailDto(group, currentMember);
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
        group.setDescription(normalizeNullableText(request.description()));
        researchGroupRepository.save(group);

        return toDetailDto(group, requesterMembership);
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
            ResearchGroupMemberRole role) {
        UserInfo inviterInfo = authApiService.findUserByEmail(authenticatedEmail);
        String normalizedInvitedEmail = EmailNormalizer.canonicalizeGoogleEmail(invitedEmail);

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

        if (memberRepository.existsActiveMemberByGroupIdAndEmail(groupId, normalizedInvitedEmail)) {
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
        entityReferenceService.attachInviterUser(invitation, inviterInfo.userId());
        if (invitedUserInfo != null) {
            entityReferenceService.attachInvitedUser(invitation, invitedUserInfo.userId());
        }
        invitation.setInvitedEmail(normalizedInvitedEmail);
        invitation.setRole(role);
        invitation.setStatus(ResearchGroupInvitationStatus.PENDING);
        invitation.setExpiresAt(Instant.now().plus(invitationExpirationDays, ChronoUnit.DAYS));
        invitation = invitationRepository.saveAndFlush(invitation);

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
        String signupUrl = frontendBaseUrl + "/auth/signup";

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
        entityReferenceService.attachUser(member, userInfo.userId());
        member.setRole(invitation.getRole());
        memberRepository.save(member);

        entityReferenceService.attachInvitedUser(invitation, userInfo.userId());
        invitation.setStatus(ResearchGroupInvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        var inviterUser = invitation.getInviterUser();
        if (inviterUser != null && !inviterUser.getId().equals(userInfo.userId())) {
            notificationService.createResearchGroupInvitationAcceptedNotification(
                    inviterUser.getId(),
                    userInfo.userId(),
                    group.getId(),
                    group.getName());
        }

        long memberCount = memberRepository.countByResearchGroupIdAndDeletedAtIsNull(group.getId());

        return toSummaryDto(group, invitation.getRole(), memberCount);
    }

    @Override
    @Transactional
    public void declineMyInvitation(String authenticatedEmail, Long invitationId) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        ResearchGroupInvitation invitation = findActivePendingInvitationForUser(invitationId, userInfo.email());
        invitation.setStatus(ResearchGroupInvitationStatus.DECLINED);
        invitationRepository.save(invitation);

        var inviterUser = invitation.getInviterUser();
        if (inviterUser != null && !inviterUser.getId().equals(userInfo.userId())) {
            ResearchGroup group = invitation.getResearchGroup();
            notificationService.createResearchGroupInvitationDeclinedNotification(
                    inviterUser.getId(),
                    userInfo.userId(),
                    group.getId(),
                    group.getName());
        }
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

        if (!memberUserId.equals(requesterInfo.userId())) {
            ResearchGroup group = targetMember.getResearchGroup();
            notificationService.createResearchGroupMemberRoleUpdatedNotification(
                    memberUserId,
                    requesterInfo.userId(),
                    group.getId(),
                    group.getName());
        }

        return memberRepository
                .findMemberDtoByGroupIdAndUserId(groupId, saved.getUser().getId())
                .orElseThrow(() -> new ResearchGroupMemberNotFoundException(groupId, memberUserId));
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

        projectApiService.removeParticipantFromAllGroupProjects(groupId, memberUserId);

        if (!memberUserId.equals(requesterInfo.userId())) {
            ResearchGroup group = targetMember.getResearchGroup();
            notificationService.createResearchGroupMemberRemovedNotification(
                    memberUserId,
                    requesterInfo.userId(),
                    group.getId(),
                    group.getName());
        }
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
        entityReferenceService.attachUser(member, userInfo.userId());
        member.setRole(ResearchGroupMemberRole.ANNOTATOR);
        memberRepository.save(member);

        resolvePendingInvitationsAfterJoinByCode(group.getId(), userInfo.email(), userInfo.userId());
        notifyGroupManagersAboutJoinByCode(group, userInfo.userId());

        long memberCount = memberRepository.countByResearchGroupIdAndDeletedAtIsNull(group.getId());

        return toSummaryDto(group, ResearchGroupMemberRole.ANNOTATOR, memberCount);
    }

    private void notifyGroupManagersAboutJoinByCode(ResearchGroup group, Long joinedUserId) {
        List<Long> managerUserIds = memberRepository.findActiveOwnerAndAdminUserIdsByGroupId(group.getId());
        for (Long managerUserId : managerUserIds) {
            if (managerUserId.equals(joinedUserId)) {
                continue;
            }
            notificationService.createResearchGroupMemberJoinedByCodeNotification(
                    managerUserId,
                    joinedUserId,
                    group.getId(),
                    group.getName());
        }
    }

    private ResearchGroupDetailDto toDetailDto(ResearchGroup group, ResearchGroupMember currentMember) {
        List<ResearchGroupMemberDto> members = memberRepository.findMembersByGroupId(group.getId());
        long activeProjects = projectApiService.countProjectsByResearchGroupId(group.getId());
        ResearchGroupMemberRole currentRole = currentMember.getRole();
        boolean isOwner = currentRole == ResearchGroupMemberRole.OWNER;

        return new ResearchGroupDetailDto(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getInvitationCode(),
                members.size(),
                activeProjects,
                group.getCreatedAt(),
                members,
                isOwner,
                isOwner || currentRole == ResearchGroupMemberRole.ADMIN);
    }

    private static String normalizeNullableText(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private static ResearchGroupSummaryDto toSummaryDto(
            ResearchGroup group,
            ResearchGroupMemberRole role,
            long memberCount) {
        return new ResearchGroupSummaryDto(
                group.getId(),
                group.getName(),
                group.getDescription(),
                role,
                memberCount,
                group.getCreatedAt());
    }

    private ResearchGroupInvitation findActivePendingInvitationForUser(Long invitationId, String normalizedEmail) {
        return invitationRepository
                .findActivePendingInvitationByIdAndInvitedEmail(invitationId, normalizedEmail, Instant.now())
                .orElseThrow(() -> new ResearchGroupInvitationNotFoundException(invitationId));
    }

    private void resolvePendingInvitationsAfterJoinByCode(Long groupId, String normalizedEmail, Long userId) {
        List<ResearchGroupInvitation> pendingInvitations = invitationRepository
                .findActivePendingInvitationsByGroupIdAndInvitedEmail(groupId, normalizedEmail, Instant.now());

        if (pendingInvitations.isEmpty()) {
            return;
        }

        pendingInvitations.forEach(invitation -> {
            entityReferenceService.attachInvitedUser(invitation, userId);
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

}
