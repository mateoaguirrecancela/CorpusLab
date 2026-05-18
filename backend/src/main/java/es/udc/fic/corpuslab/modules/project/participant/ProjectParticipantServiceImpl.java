package es.udc.fic.corpuslab.modules.project.participant;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.notification.services.EmailService;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.metrics.ProjectMetricsCacheService;
import es.udc.fic.corpuslab.modules.project.participant.dtos.AssignProjectParticipantsRequestDto;
import es.udc.fic.corpuslab.modules.project.participant.dtos.ProjectAssignableMemberDto;
import es.udc.fic.corpuslab.modules.project.participant.dtos.ProjectAssignmentContextDto;
import es.udc.fic.corpuslab.modules.project.participant.dtos.ProjectParticipantAssignmentDto;
import es.udc.fic.corpuslab.modules.project.shared.references.ProjectEntityReferenceService;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantIaaGroup;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectParticipantsException;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.ProjectNotFoundException;
import es.udc.fic.corpuslab.modules.project.shared.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectConstants;
import es.udc.fic.corpuslab.modules.researchgroup.api.ResearchGroupApiService;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupMemberInfo;

@Service
public class ProjectParticipantServiceImpl implements ProjectParticipantService {

    private final ProjectParticipantRepository projectParticipantRepository;
    private final ProjectRepository projectRepository;
    private final AnnotationRepository annotationRepository;
    private final AuthApiService authApiService;
    private final ResearchGroupApiService researchGroupApiService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final ProjectMetricsCacheService projectMetricsCacheService;
    private final ProjectEntityReferenceService entityReferenceService;
    private final ProjectParticipantAssignmentPolicy assignmentPolicy;
    private final IaaGroupAssignmentPolicy iaaGroupAssignmentPolicy;
    private final String frontendBaseUrl;

    @Autowired
    public ProjectParticipantServiceImpl(
            ProjectParticipantRepository projectParticipantRepository,
            ProjectRepository projectRepository,
            AnnotationRepository annotationRepository,
            AuthApiService authApiService,
            ResearchGroupApiService researchGroupApiService,
            NotificationService notificationService,
            EmailService emailService,
            ProjectMetricsCacheService projectMetricsCacheService,
            ProjectEntityReferenceService entityReferenceService,
            ProjectParticipantAssignmentPolicy assignmentPolicy,
            IaaGroupAssignmentPolicy iaaGroupAssignmentPolicy,
            @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.projectParticipantRepository = projectParticipantRepository;
        this.projectRepository = projectRepository;
        this.annotationRepository = annotationRepository;
        this.authApiService = authApiService;
        this.researchGroupApiService = researchGroupApiService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.projectMetricsCacheService = projectMetricsCacheService;
        this.entityReferenceService = entityReferenceService;
        this.assignmentPolicy = assignmentPolicy;
        this.iaaGroupAssignmentPolicy = iaaGroupAssignmentPolicy;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    @Transactional
    public void assignParticipants(String authenticatedEmail, Long researchGroupId, Long projectId,
            List<Long> participantUserIds) {
        assignParticipants(
                authenticatedEmail,
                researchGroupId,
                projectId,
                new AssignProjectParticipantsRequestDto(participantUserIds));
    }

    @Override
    @Transactional
    public void assignParticipants(String authenticatedEmail, Long researchGroupId, Long projectId,
            AssignProjectParticipantsRequestDto request) {
        UserInfo requesterInfo = authApiService.findUserByEmail(authenticatedEmail);

        projectRepository.findByIdAndResearchGroupId(projectId, researchGroupId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        ResearchGroupMemberInfo requesterMembership = researchGroupApiService
                .findActiveMember(researchGroupId, requesterInfo.userId())
                .orElseThrow(() -> new AccessDeniedException(ProjectConstants.NOT_MEMBER_ERROR));

        if (!requesterMembership.isOwnerOrAdmin()) {
            throw new AccessDeniedException("Only owners or admins can assign investigators");
        }

        boolean usesGroupedAssignments = request != null
                && request.participantAssignments() != null
                && !request.participantAssignments().isEmpty();
        List<NormalizedParticipantAssignment> assignments = usesGroupedAssignments
                ? normalizeGroupedAssignments(request.participantAssignments())
                : normalizeFlatAssignments(request == null ? null : request.participantUserIds());

        replaceProjectParticipants(projectId, requesterInfo.userId(), researchGroupId, assignments,
                usesGroupedAssignments);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectAssignmentContextDto getProjectAssignmentContext(String authenticatedEmail, Long researchGroupId) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        researchGroupApiService.findGroupById(researchGroupId)
                .orElseThrow(() -> new AccessDeniedException(ProjectConstants.NOT_MEMBER_ERROR));

        ResearchGroupMemberInfo requesterMembership = researchGroupApiService
                .findActiveMember(researchGroupId, userInfo.userId())
                .orElseThrow(() -> new AccessDeniedException(ProjectConstants.NOT_MEMBER_ERROR));

        if (!requesterMembership.isOwnerOrAdmin()) {
            throw new AccessDeniedException("Only owners or admins can assign investigators");
        }

        Set<Long> activeMemberIds = new LinkedHashSet<>(
                researchGroupApiService.findActiveMemberUserIds(researchGroupId));
        List<ProjectAssignableMemberDto> members = authApiService.findUsersByIds(activeMemberIds)
                .stream()
                .sorted(assignableMemberComparator(userInfo.userId()))
                .map(this::toAssignableMemberDto)
                .toList();

        return new ProjectAssignmentContextDto(
                researchGroupId,
                userInfo.userId(),
                members,
                List.of(new ProjectParticipantAssignmentDto(
                        userInfo.userId(),
                        ProjectParticipantIaaGroup.GROUP_A)));
    }

    @Override
    @Transactional
    public void replaceProjectParticipants(
            Long projectId,
            Long requesterUserId,
            Long researchGroupId,
            List<Long> participantUserIds) {
        replaceProjectParticipants(
                projectId,
                requesterUserId,
                researchGroupId,
                normalizeFlatAssignments(participantUserIds),
                false);
    }

    @Override
    @Transactional
    public void replaceProjectParticipantAssignments(
            Long projectId,
            Long requesterUserId,
            Long researchGroupId,
            List<ProjectParticipantAssignmentDto> participantAssignments) {
        replaceProjectParticipants(
                projectId,
                requesterUserId,
                researchGroupId,
                normalizeGroupedAssignments(participantAssignments),
                true);
    }

    private void replaceProjectParticipants(
            Long projectId,
            Long requesterUserId,
            Long researchGroupId,
            List<NormalizedParticipantAssignment> requestedAssignments,
            boolean overwriteIaaGroups) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        Map<Long, ProjectParticipantIaaGroup> requestedGroupsByUserId = new LinkedHashMap<>();
        for (NormalizedParticipantAssignment assignment : requestedAssignments) {
            requestedGroupsByUserId.putIfAbsent(assignment.userId(), assignment.iaaGroup());
        }
        List<Long> requestedIds = List.copyOf(requestedGroupsByUserId.keySet());

        Set<Long> activeMemberIds = new HashSet<>(
                researchGroupApiService.findActiveMemberUserIds(researchGroupId));

        assignmentPolicy.requireActiveResearchGroupMembers(requestedIds, activeMemberIds);

        List<ProjectParticipant> allExistingParticipants = projectParticipantRepository
                .findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(projectId);
        ProjectParticipant creatorParticipant = allExistingParticipants.stream()
                .filter(projectParticipant -> projectParticipant.getRole() == ProjectParticipantRole.CREATOR)
                .findFirst()
                .orElse(null);
        Long creatorUserId = creatorParticipant != null ? creatorParticipant.getUser().getId() : requesterUserId;

        if (overwriteIaaGroups && creatorParticipant != null) {
            ProjectParticipantIaaGroup requestedCreatorGroup = requestedGroupsByUserId.get(creatorUserId);
            ProjectParticipantIaaGroup nextCreatorGroup = iaaGroupAssignmentPolicy.defaultCreatorGroup(
                    requestedCreatorGroup);
            creatorParticipant.setIaaGroup(nextCreatorGroup);
            projectParticipantRepository.save(creatorParticipant);
        }

        List<Long> filteredIds = requestedIds.stream()
                .filter(id -> !id.equals(creatorUserId))
                .toList();
        Map<Long, ProjectParticipantIaaGroup> filteredGroupsByUserId = new LinkedHashMap<>();
        for (Long id : filteredIds) {
            filteredGroupsByUserId.put(id, requestedGroupsByUserId.get(id));
        }

        Set<Long> existingParticipantIds = allExistingParticipants.stream()
                .filter(projectParticipant -> projectParticipant.getRole() == ProjectParticipantRole.PARTICIPANT)
                .map(projectParticipant -> projectParticipant.getUser().getId())
                .collect(Collectors.toSet());

        Map<Long, ProjectParticipant> existingParticipantsByUserId = allExistingParticipants.stream()
                .filter(projectParticipant -> projectParticipant.getRole() == ProjectParticipantRole.PARTICIPANT)
                .collect(Collectors.toMap(
                        projectParticipant -> projectParticipant.getUser().getId(),
                        projectParticipant -> projectParticipant,
                        (first, ignored) -> first,
                        LinkedHashMap::new));

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

        if (overwriteIaaGroups) {
            List<ProjectParticipant> participantsToUpdate = filteredIds.stream()
                    .map(existingParticipantsByUserId::get)
                    .filter(participant -> participant != null)
                    .peek(participant -> participant.setIaaGroup(
                            filteredGroupsByUserId.get(participant.getUser().getId())))
                    .toList();

            if (!participantsToUpdate.isEmpty()) {
                projectParticipantRepository.saveAll(participantsToUpdate);
            }
        }

        Set<Long> newParticipantIds = filteredIds.stream()
                .filter(id -> !existingParticipantIds.contains(id))
                .collect(Collectors.toSet());

        if (newParticipantIds.isEmpty()) {
            projectMetricsCacheService.evictProjectReadCaches(projectId);
            return;
        }

        List<UserInfo> usersToAssign = authApiService.findUsersByIds(newParticipantIds);
        if (usersToAssign.size() != newParticipantIds.size()) {
            throw new InvalidProjectParticipantsException("Some selected investigators do not exist");
        }

        List<ProjectParticipant> participants = usersToAssign.stream().map(userInfo -> {
            ProjectParticipant participant = new ProjectParticipant();
            participant.setProject(project);
            entityReferenceService.attachUser(participant, userInfo.userId());
            participant.setRole(ProjectParticipantRole.PARTICIPANT);
            participant.setIaaGroup(filteredGroupsByUserId.get(userInfo.userId()));
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
        projectMetricsCacheService.evictProjectReadCaches(projectId);
    }

    private List<NormalizedParticipantAssignment> normalizeFlatAssignments(List<Long> participantUserIds) {
        if (participantUserIds == null || participantUserIds.isEmpty()) {
            return List.of();
        }

        return participantUserIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .map(id -> new NormalizedParticipantAssignment(id, null))
                .toList();
    }

    private List<NormalizedParticipantAssignment> normalizeGroupedAssignments(
            List<ProjectParticipantAssignmentDto> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return List.of();
        }

        Map<Long, ProjectParticipantIaaGroup> groupsByUserId = new LinkedHashMap<>();
        for (ProjectParticipantAssignmentDto assignment : assignments) {
            if (assignment == null || assignment.userId() == null || assignment.userId() <= 0) {
                continue;
            }
            groupsByUserId.putIfAbsent(assignment.userId(), assignment.iaaGroup());
        }

        return groupsByUserId.entrySet().stream()
                .map(entry -> new NormalizedParticipantAssignment(entry.getKey(), entry.getValue()))
                .toList();
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
            projectParticipantRepository
                    .findByProjectIdAndUserId(project.getId(), userId)
                    .filter(participant -> participant.getRole() == ProjectParticipantRole.PARTICIPANT)
                    .ifPresent(participant -> {
                        removeStoredAnnotationsForUsers(project.getId(), Set.of(userId));
                        projectParticipantRepository.delete(participant);
                        projectMetricsCacheService.evictProjectReadCaches(project.getId());
                    });
        }
    }

    private Comparator<UserInfo> assignableMemberComparator(Long currentUserId) {
        return Comparator
                .comparing((UserInfo user) -> !user.userId().equals(currentUserId))
                .thenComparing(user -> normalizedSortText(user.lastName()))
                .thenComparing(user -> normalizedSortText(user.firstName()))
                .thenComparing(UserInfo::userId);
    }

    private static String normalizedSortText(String value) {
        String normalizedValue = StringUtils.trimToNull(value);
        return normalizedValue == null ? "" : normalizedValue.toLowerCase();
    }

    private ProjectAssignableMemberDto toAssignableMemberDto(UserInfo userInfo) {
        return new ProjectAssignableMemberDto(
                userInfo.userId(),
                userInfo.firstName(),
                userInfo.lastName(),
                userInfo.email());
    }

    private record NormalizedParticipantAssignment(
            Long userId,
            ProjectParticipantIaaGroup iaaGroup) {
    }
}
