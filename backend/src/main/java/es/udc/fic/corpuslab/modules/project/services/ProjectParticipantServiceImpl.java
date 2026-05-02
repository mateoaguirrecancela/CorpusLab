package es.udc.fic.corpuslab.modules.project.services;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.notification.services.EmailService;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.exceptions.InvalidProjectParticipantsException;
import es.udc.fic.corpuslab.modules.project.exceptions.ProjectNotFoundException;
import es.udc.fic.corpuslab.modules.project.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.project.utils.ProjectConstants;
import es.udc.fic.corpuslab.modules.researchgroup.api.ResearchGroupApiService;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupMemberInfo;
import jakarta.persistence.EntityManager;

@Service
public class ProjectParticipantServiceImpl implements ProjectParticipantService {

    private final ProjectParticipantRepository projectParticipantRepository;
    private final ProjectRepository projectRepository;
    private final AnnotationRepository annotationRepository;
    private final AuthApiService authApiService;
    private final ResearchGroupApiService researchGroupApiService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final EntityManager entityManager;
    private final String frontendBaseUrl;

    public ProjectParticipantServiceImpl(
            ProjectParticipantRepository projectParticipantRepository,
            ProjectRepository projectRepository,
            AnnotationRepository annotationRepository,
            AuthApiService authApiService,
            ResearchGroupApiService researchGroupApiService,
            NotificationService notificationService,
            EmailService emailService,
            EntityManager entityManager,
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.projectParticipantRepository = projectParticipantRepository;
        this.projectRepository = projectRepository;
        this.annotationRepository = annotationRepository;
        this.authApiService = authApiService;
        this.researchGroupApiService = researchGroupApiService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.entityManager = entityManager;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    @Transactional
    public void assignParticipants(String authenticatedEmail, Long researchGroupId, Long projectId,
            List<Long> participantUserIds) {
        UserInfo requesterInfo = authApiService.findUserByEmail(authenticatedEmail);

        projectRepository.findByIdAndResearchGroupId(projectId, researchGroupId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        ResearchGroupMemberInfo requesterMembership = researchGroupApiService
                .findActiveMember(researchGroupId, requesterInfo.userId())
                .orElseThrow(() -> new AccessDeniedException(ProjectConstants.NOT_MEMBER_ERROR));

        if (!requesterMembership.isOwnerOrAdmin()) {
            throw new AccessDeniedException("Only owners or admins can assign investigators");
        }

        replaceProjectParticipants(projectId, requesterInfo.userId(), researchGroupId, participantUserIds);
    }

    @Override
    @Transactional
    public void replaceProjectParticipants(
            Long projectId,
            Long requesterUserId,
            Long researchGroupId,
            List<Long> participantUserIds) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        List<Long> requestedIds = participantUserIds == null
                ? List.of()
                : participantUserIds.stream()
                        .filter(id -> id != null && id > 0)
                        .distinct()
                        .toList();

        Set<Long> activeMemberIds = new HashSet<>(
                researchGroupApiService.findActiveMemberUserIds(researchGroupId));

        boolean hasIdsOutsideGroup = requestedIds.stream().anyMatch(id -> !activeMemberIds.contains(id));
        if (hasIdsOutsideGroup) {
            throw new InvalidProjectParticipantsException(
                    "All selected investigators must be active members of the research group");
        }

        List<Long> filteredIds = requestedIds.stream()
                .filter(id -> !id.equals(requesterUserId))
                .toList();

        List<ProjectParticipant> allExistingParticipants = projectParticipantRepository
                .findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(projectId);

        Set<Long> existingParticipantIds = allExistingParticipants.stream()
                .filter(projectParticipant -> projectParticipant.getRole() == ProjectParticipantRole.PARTICIPANT)
                .map(projectParticipant -> projectParticipant.getUser().getId())
                .collect(Collectors.toSet());

        Set<Long> filteredParticipantIds = new HashSet<>(filteredIds);
        Set<Long> removedParticipantIds = existingParticipantIds.stream()
                .filter(existingId -> !filteredParticipantIds.contains(existingId))
                .collect(Collectors.toSet());

        removeStoredAnnotationsForUsers(projectId, removedParticipantIds);

        List<ProjectParticipant> participantsToRemove = allExistingParticipants.stream()
                .filter(p -> p.getRole() == ProjectParticipantRole.PARTICIPANT
                        && removedParticipantIds.contains(p.getUser().getId()))
                .toList();

        if (!participantsToRemove.isEmpty()) {
            projectParticipantRepository.deleteAll(participantsToRemove);
        }

        Set<Long> newParticipantIds = filteredIds.stream()
                .filter(id -> !existingParticipantIds.contains(id))
                .collect(Collectors.toSet());

        if (newParticipantIds.isEmpty()) {
            return;
        }

        List<UserInfo> usersToAssign = authApiService.findUsersByIds(newParticipantIds);
        if (usersToAssign.size() != newParticipantIds.size()) {
            throw new InvalidProjectParticipantsException("Some selected investigators do not exist");
        }

        List<ProjectParticipant> participants = usersToAssign.stream().map(userInfo -> {
            ProjectParticipant participant = new ProjectParticipant();
            participant.setProject(project);
            participant.setUser(getUserReference(userInfo.userId()));
            participant.setRole(ProjectParticipantRole.PARTICIPANT);
            return participant;
        }).toList();

        projectParticipantRepository.saveAll(participants);

        UserInfo requesterInfo = authApiService.findUserById(requesterUserId);
        String assignerFullName = requesterInfo.fullName();
        String projectUrl = frontendBaseUrl + "/home/projects/" + project.getId();

        Long researchGroupId2 = project.getResearchGroup().getId();
        String researchGroupName = project.getResearchGroup().getName();

        for (UserInfo recipient : usersToAssign) {
            notificationService.createProjectParticipantAssignedNotification(
                    recipient.userId(),
                    requesterUserId,
                    project.getId(),
                    project.getName(),
                    researchGroupId2,
                    researchGroupName);
            emailService.sendProjectAssignmentEmail(
                    recipient.email(),
                    project.getName(),
                    assignerFullName,
                    projectUrl);
        }
    }

    private void removeStoredAnnotationsForUsers(Long projectId, Set<Long> removedUserIds) {
        if (removedUserIds == null || removedUserIds.isEmpty()) {
            return;
        }

        annotationRepository.deleteByDatasetItemProjectIdAndUserIdIn(projectId, removedUserIds);
    }

    @Override
    @Transactional
    public void removeParticipantFromAllGroupProjects(Long researchGroupId, Long userId) {
        List<Project> projects = projectRepository.findByResearchGroupId(researchGroupId);
        for (Project project : projects) {
            Optional<ProjectParticipant> participantOpt = projectParticipantRepository
                    .findByProjectIdAndUserId(project.getId(), userId);

            if (participantOpt.isPresent()) {
                ProjectParticipant participant = participantOpt.get();
                if (participant.getRole() == ProjectParticipantRole.PARTICIPANT) {
                    removeStoredAnnotationsForUsers(project.getId(), Set.of(userId));
                    projectParticipantRepository.delete(participant);
                }
            }
        }
    }

    /**
     * Creates a JPA proxy reference for User without loading the entity.
     * Used only to satisfy @ManyToOne FK relationship on ProjectParticipant.
     */
    private User getUserReference(Long userId) {
        return entityManager.getReference(User.class, userId);
    }
}
