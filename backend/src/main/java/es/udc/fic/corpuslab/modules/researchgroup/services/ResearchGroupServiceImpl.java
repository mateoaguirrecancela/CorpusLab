package es.udc.fic.corpuslab.modules.researchgroup.services;

import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.auth.utils.EmailNormalizer;
import es.udc.fic.corpuslab.modules.notification.services.EmailService;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.CreateResearchGroupRequestDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupInvitationDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupDetailDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupMemberDto;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupSummaryDto;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupInvitation;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupMember;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupInvitationStatus;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.InvalidResearchGroupInvitationRoleException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupInvitationAlreadyExistsException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupInvitationCodeNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupInvitationEmailDeliveryException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupMemberAlreadyExistsException;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupInvitationRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;

@Service
public class ResearchGroupServiceImpl implements ResearchGroupService {

        private final UserRepository userRepository;
        private final ResearchGroupRepository researchGroupRepository;
        private final ResearchGroupMemberRepository memberRepository;
        private final ResearchGroupInvitationRepository invitationRepository;
        private final EmailService emailService;
        private final String frontendBaseUrl;

        public ResearchGroupServiceImpl(
                        UserRepository userRepository,
                        ResearchGroupRepository researchGroupRepository,
                        ResearchGroupMemberRepository memberRepository,
                        ResearchGroupInvitationRepository invitationRepository,
                        EmailService emailService,
                        @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
                this.userRepository = userRepository;
                this.researchGroupRepository = researchGroupRepository;
                this.memberRepository = memberRepository;
                this.invitationRepository = invitationRepository;
                this.emailService = emailService;
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

                return new ResearchGroupDetailDto(
                                group.getId(),
                                group.getName(),
                                group.getDescription(),
                                group.getInvitationCode(),
                                members.size(),
                                0L,
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
                invitation.setToken(UUID.randomUUID().toString());
                invitation.setRole(role);
                invitation.setStatus(ResearchGroupInvitationStatus.PENDING);
                invitation.setExpiresAt(expiresAt);
                invitation = invitationRepository.save(invitation);

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

                long memberCount = memberRepository.findActiveMemberEmailsByGroupId(group.getId()).size();

                return new ResearchGroupSummaryDto(
                                group.getId(),
                                group.getName(),
                                group.getDescription(),
                                ResearchGroupMemberRole.ANNOTATOR,
                                memberCount,
                                group.getCreatedAt());
        }
}
