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

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.auth.utils.EmailNormalizer;
import es.udc.fic.corpuslab.modules.notification.services.EmailService;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;
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

        private static final int MAX_TOKEN_GENERATION_ATTEMPTS = 10;

        private final UserRepository userRepository;
        private final ResearchGroupRepository researchGroupRepository;
        private final ResearchGroupMemberRepository memberRepository;
        private final ResearchGroupInvitationRepository invitationRepository;
        private final ProjectRepository projectRepository;
        private final EmailService emailService;
        private final NotificationService notificationService;
        private final String frontendBaseUrl;

        public ResearchGroupServiceImpl(
                        UserRepository userRepository,
                        ResearchGroupRepository researchGroupRepository,
                        ResearchGroupMemberRepository memberRepository,
                        ResearchGroupInvitationRepository invitationRepository,
                        ProjectRepository projectRepository,
                        EmailService emailService,
                        NotificationService notificationService,
                        @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
                this.userRepository = userRepository;
                this.researchGroupRepository = researchGroupRepository;
                this.memberRepository = memberRepository;
                this.invitationRepository = invitationRepository;
                this.projectRepository = projectRepository;
                this.emailService = emailService;
                this.notificationService = notificationService;
                this.frontendBaseUrl = frontendBaseUrl;
        }

        @Override
        @Transactional(readOnly = true)
        public List<ResearchGroupSummaryDto> findMyResearchGroups(String authenticatedEmail) {
                String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);

                User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));

                return memberRepository.findGroupSummariesByUserId(user.getId());
        }

        @Override
        @Transactional
        public ResearchGroupSummaryDto createResearchGroup(String authenticatedEmail,
                        CreateResearchGroupRequestDto request) {
                String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);

                User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));

                ResearchGroup group = new ResearchGroup();
                group.setName(request.name().trim());
                group.setDescription(
                                request.description() != null && !request.description().isBlank()
                                                ? request.description().trim()
                                                : null);
                group = researchGroupRepository.save(group);

                ResearchGroupMember ownerMember = new ResearchGroupMember();
                ownerMember.setUser(user);
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
                String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);

                User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));

                ResearchGroup group = researchGroupRepository.findById(groupId)
                                .orElseThrow(() -> new ResearchGroupNotFoundException(groupId));

                List<ResearchGroupMemberDto> members = memberRepository.findMembersByGroupId(groupId);
                boolean isMember = members.stream().anyMatch(m -> m.userId().equals(user.getId()));

                if (!isMember) {
                        throw new AccessDeniedException("User is not a member of this research group");
                }

                long activeProjects = projectRepository.countByResearchGroupId(groupId);

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
                User requester = findUserByEmail(authenticatedEmail);

                ResearchGroup group = researchGroupRepository.findById(groupId)
                                .orElseThrow(() -> new ResearchGroupNotFoundException(groupId));

                ResearchGroupMember requesterMembership = memberRepository
                                .findActiveMemberByGroupIdAndUserId(groupId, requester.getId())
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
                long activeProjects = projectRepository.countByResearchGroupId(groupId);

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
        public ResearchGroupInvitationDto inviteResearcherByEmail(
                        String authenticatedEmail,
                        Long groupId,
                        String invitedEmail,
                        ResearchGroupMemberRole role,
                        Instant expiresAt) {
                String normalizedAuthenticatedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);
                String normalizedInvitedEmail = EmailNormalizer.canonicalizeGoogleEmail(invitedEmail)
                                .toLowerCase(Locale.ROOT);

                if (role == ResearchGroupMemberRole.OWNER) {
                        throw new InvalidResearchGroupInvitationRoleException(role);
                }

                User inviter = userRepository.findByEmailIgnoreCase(normalizedAuthenticatedEmail)
                                .orElseThrow(() -> new EmailNotFoundException(normalizedAuthenticatedEmail));

                ResearchGroup group = researchGroupRepository.findById(groupId)
                                .orElseThrow(() -> new ResearchGroupNotFoundException(groupId));

                ResearchGroupMember inviterMembership = memberRepository
                                .findActiveMemberByGroupIdAndUserId(groupId, inviter.getId())
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

                User invitedUser = userRepository.findByEmailIgnoreCase(normalizedInvitedEmail).orElse(null);

                ResearchGroupInvitation invitation = new ResearchGroupInvitation();
                invitation.setResearchGroup(group);
                invitation.setInviterUser(inviter);
                invitation.setInvitedUser(invitedUser);
                invitation.setInvitedEmail(normalizedInvitedEmail);
                invitation.setRole(role);
                invitation.setStatus(ResearchGroupInvitationStatus.PENDING);
                invitation.setExpiresAt(expiresAt);
                invitation = saveInvitationWithUniqueToken(invitation);

                if (invitedUser != null) {
                        notificationService.createResearchGroupInvitationReceivedNotification(
                                        invitedUser,
                                        inviter,
                                        group,
                                        invitation.getId());
                }

                String inviterFullName = (inviter.getFirstName() + " " + inviter.getLastName()).trim();
                String invitationUrl = frontendBaseUrl + "/home/invitations?token=" + invitation.getToken();
                String signupUrl = frontendBaseUrl + "/auth/signup?invitationToken=" + invitation.getToken();

                try {
                        if (invitedUser != null) {
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
                String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);

                userRepository.findByEmailIgnoreCase(normalizedEmail)
                                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));

                return invitationRepository.findPendingInvitationsByInvitedEmail(normalizedEmail, Instant.now());
        }

        @Override
        @Transactional
        public ResearchGroupSummaryDto acceptMyInvitation(String authenticatedEmail, Long invitationId) {
                String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);

                User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));

                ResearchGroupInvitation invitation = findActivePendingInvitationForUser(invitationId, normalizedEmail);
                ResearchGroup group = invitation.getResearchGroup();

                if (memberRepository.findActiveMemberByGroupIdAndUserId(group.getId(), user.getId()).isPresent()) {
                        throw new ResearchGroupMemberAlreadyExistsException(group.getId(), normalizedEmail);
                }

                ResearchGroupMember member = new ResearchGroupMember();
                member.setResearchGroup(group);
                member.setUser(user);
                member.setRole(invitation.getRole());
                memberRepository.save(member);

                invitation.setInvitedUser(user);
                invitation.setStatus(ResearchGroupInvitationStatus.ACCEPTED);
                invitationRepository.save(invitation);

                User inviter = invitation.getInviterUser();
                if (inviter != null && !inviter.getId().equals(user.getId())) {
                        notificationService.createResearchGroupInvitationAcceptedNotification(
                                        inviter,
                                        user,
                                        group);
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
                String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);

                userRepository.findByEmailIgnoreCase(normalizedEmail)
                                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));

                ResearchGroupInvitation invitation = findActivePendingInvitationForUser(invitationId, normalizedEmail);
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

                User requester = findUserByEmail(authenticatedEmail);
                validateOwnerPermissions(groupId, requester.getId());

                ResearchGroupMember targetMember = memberRepository
                                .findActiveMemberByGroupIdAndUserId(groupId, memberUserId)
                                .orElseThrow(() -> new ResearchGroupMemberNotFoundException(groupId, memberUserId));

                if (targetMember.getRole() == ResearchGroupMemberRole.OWNER) {
                        throw new InvalidResearchGroupMemberRoleException(targetMember.getRole());
                }

                targetMember.setRole(role);
                ResearchGroupMember saved = memberRepository.save(targetMember);

                return new ResearchGroupMemberDto(
                                saved.getUser().getId(),
                                saved.getUser().getFirstName(),
                                saved.getUser().getLastName(),
                                saved.getUser().getEmail(),
                                saved.getRole());
        }

        @Override
        @Transactional
        public void removeMember(String authenticatedEmail, Long groupId, Long memberUserId) {
                User requester = findUserByEmail(authenticatedEmail);
                validateOwnerPermissions(groupId, requester.getId());

                ResearchGroupMember targetMember = memberRepository
                                .findActiveMemberByGroupIdAndUserId(groupId, memberUserId)
                                .orElseThrow(() -> new ResearchGroupMemberNotFoundException(groupId, memberUserId));

                if (targetMember.getRole() == ResearchGroupMemberRole.OWNER) {
                        throw new InvalidResearchGroupMemberRoleException(targetMember.getRole());
                }

                targetMember.setDeletedAt(Instant.now());
                memberRepository.save(targetMember);
        }

        @Override
        @Transactional
        public ResearchGroupSummaryDto joinResearchGroupByCode(String authenticatedEmail, String invitationCode) {
                String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);
                String normalizedCode = invitationCode.trim().toUpperCase(Locale.ROOT);

                User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));

                ResearchGroup group = researchGroupRepository.findByInvitationCodeIgnoreCase(normalizedCode)
                                .orElseThrow(() -> new ResearchGroupInvitationCodeNotFoundException(normalizedCode));

                if (memberRepository.findActiveMemberByGroupIdAndUserId(group.getId(), user.getId()).isPresent()) {
                        throw new ResearchGroupMemberAlreadyExistsException(group.getId(), normalizedEmail);
                }

                ResearchGroupMember member = new ResearchGroupMember();
                member.setResearchGroup(group);
                member.setUser(user);
                member.setRole(ResearchGroupMemberRole.ANNOTATOR);
                memberRepository.save(member);

                resolvePendingInvitationsAfterJoinByCode(group.getId(), normalizedEmail, user);

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
                                .findActivePendingInvitationByIdAndInvitedEmail(invitationId, normalizedEmail,
                                                Instant.now())
                                .orElseThrow(() -> new ResearchGroupInvitationNotFoundException(invitationId));
        }

        private void resolvePendingInvitationsAfterJoinByCode(Long groupId, String normalizedEmail, User user) {
                List<ResearchGroupInvitation> pendingInvitations = invitationRepository
                                .findActivePendingInvitationsByGroupIdAndInvitedEmail(groupId, normalizedEmail,
                                                Instant.now());

                if (pendingInvitations.isEmpty()) {
                        return;
                }

                pendingInvitations.forEach(invitation -> {
                        invitation.setInvitedUser(user);
                        invitation.setStatus(ResearchGroupInvitationStatus.ACCEPTED);
                });

                invitationRepository.saveAll(pendingInvitations);
        }

        private User findUserByEmail(String authenticatedEmail) {
                String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);
                return userRepository.findByEmailIgnoreCase(normalizedEmail)
                                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));
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
