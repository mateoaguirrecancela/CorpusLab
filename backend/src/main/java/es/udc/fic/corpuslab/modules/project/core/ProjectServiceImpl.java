package es.udc.fic.corpuslab.modules.project.core;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.core.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dataset.dtos.DatasetItemDto;
import es.udc.fic.corpuslab.modules.project.dataset.dtos.DatasetItemSummaryProjection;
import es.udc.fic.corpuslab.modules.project.core.dtos.ProjectAssignedSummaryDto;
import es.udc.fic.corpuslab.modules.project.core.dtos.ProjectDetailDto;
import es.udc.fic.corpuslab.modules.project.participant.dtos.ProjectDetailParticipantDto;
import es.udc.fic.corpuslab.modules.project.guideline.ProjectGuidelineService;
import es.udc.fic.corpuslab.modules.project.metrics.ProjectMetricsCacheService;
import es.udc.fic.corpuslab.modules.project.participant.ProjectParticipantService;
import es.udc.fic.corpuslab.modules.project.progress.ProjectProgressCalculator;
import es.udc.fic.corpuslab.modules.project.progress.ProjectProgressSnapshot;
import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.core.dtos.ProjectSummaryDto;
import es.udc.fic.corpuslab.modules.project.core.dtos.UpdateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.shared.entities.Guideline;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantIaaGroup;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.ProjectNotFoundException;
import es.udc.fic.corpuslab.modules.project.shared.references.ProjectEntityReferenceService;
import es.udc.fic.corpuslab.modules.project.shared.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.api.ResearchGroupApiService;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupMemberInfo;

import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectConstants;

@Service
public class ProjectServiceImpl implements ProjectService {

    private static final int DEFAULT_PROJECT_PAGE_SIZE = 12;
    private static final int MAX_PROJECT_PAGE_SIZE = 50;
    private final ProjectRepository projectRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final AnnotationRepository annotationRepository;
    private final AuthApiService authApiService;
    private final ResearchGroupApiService researchGroupApiService;
    private final NotificationService notificationService;
    private final ProjectParticipantService projectParticipantService;
    private final ProjectMetricsCacheService projectMetricsCacheService;
    private final ProjectEntityReferenceService entityReferenceService;
    private final ProjectProgressCalculator projectProgressCalculator;
    private final ProjectGuidelineService projectGuidelineService;
    private final Duration wizardCleanupWindow;

    @Autowired
    public ProjectServiceImpl(
            ProjectRepository projectRepository,
            ProjectParticipantRepository projectParticipantRepository,
            DatasetItemRepository datasetItemRepository,
            AnnotationRepository annotationRepository,
            AuthApiService authApiService,
            ResearchGroupApiService researchGroupApiService,
            NotificationService notificationService,
            ProjectParticipantService projectParticipantService,
            ProjectMetricsCacheService projectMetricsCacheService,
            ProjectEntityReferenceService entityReferenceService,
            ProjectProgressCalculator projectProgressCalculator,
            ProjectGuidelineService projectGuidelineService,
            @Value("${app.project.wizard-cleanup-window-minutes:30}") long wizardCleanupWindowMinutes) {
        this.projectRepository = projectRepository;
        this.projectParticipantRepository = projectParticipantRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.annotationRepository = annotationRepository;
        this.authApiService = authApiService;
        this.researchGroupApiService = researchGroupApiService;
        this.notificationService = notificationService;
        this.projectParticipantService = projectParticipantService;
        this.projectMetricsCacheService = projectMetricsCacheService;
        this.entityReferenceService = entityReferenceService;
        this.projectProgressCalculator = projectProgressCalculator;
        this.projectGuidelineService = projectGuidelineService;
        this.wizardCleanupWindow = Duration.ofMinutes(Math.max(1L, wizardCleanupWindowMinutes));
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ProjectAssignedSummaryDto> findAssignedProjectsByResearchGroup(String authenticatedEmail,
            Long researchGroupId, int page, int size, boolean showArchived) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        researchGroupApiService
                .findActiveMember(researchGroupId, userInfo.userId())
                .orElseThrow(() -> new AccessDeniedException(ProjectConstants.NOT_MEMBER_ERROR));

        Pageable pageable = PageRequest.of(sanitizePage(page), sanitizePageSize(size));
        Slice<ProjectParticipant> participants = showArchived
                ? projectParticipantRepository
                        .findByProjectResearchGroupIdAndUserIdAndProjectArchivedTrueOrderByProjectCreatedAtDesc(
                                researchGroupId,
                                userInfo.userId(),
                                pageable)
                : projectParticipantRepository
                        .findByProjectResearchGroupIdAndUserIdAndProjectArchivedFalseOrderByProjectCreatedAtDesc(
                                researchGroupId,
                                userInfo.userId(),
                                pageable);

        return participants.map(this::toAssignedSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ProjectAssignedSummaryDto> findMyAssignedProjects(String authenticatedEmail, int page, int size,
            boolean showArchived) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);
        Pageable pageable = PageRequest.of(sanitizePage(page), sanitizePageSize(size));

        Slice<ProjectParticipant> participants = showArchived
                ? projectParticipantRepository.findByUserIdAndProjectArchivedTrueOrderByProjectCreatedAtDesc(
                        userInfo.userId(),
                        pageable)
                : projectParticipantRepository.findByUserIdAndProjectArchivedFalseOrderByProjectCreatedAtDesc(
                        userInfo.userId(),
                        pageable);

        return participants.map(this::toAssignedSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDetailDto getAssignedProjectDetail(String authenticatedEmail, Long projectId) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        ProjectParticipant participant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, userInfo.userId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        Project project = participant.getProject();
        Guideline guideline = project.getGuideline();

        List<ProjectSetupLabelDto> labels = project.getLabels().stream()
                .map(label -> new ProjectSetupLabelDto(label.getName(), label.getColor()))
                .toList();

        String guidelineText = guideline != null ? guideline.getContent() : null;
        String guidelinePdfBase64 = guideline != null ? guideline.getFileUrl() : null;
        long guidelinePdfSizeBytes = projectGuidelineService.calculateGuidelinePdfSizeBytes(guidelinePdfBase64);
        boolean guidelinePdfAvailable = guidelinePdfSizeBytes > 0;
        boolean canManageProject = participant.getRole() == ProjectParticipantRole.CREATOR;

        List<DatasetItemSummaryProjection> datasetItemSummaries = datasetItemRepository
                .findSummariesByProjectId(projectId);
        List<DatasetItemDto> datasetItemDtos = datasetItemSummaries.stream()
                .map(this::toDatasetItemDto)
                .toList();
        List<ProjectParticipant> projectParticipants = projectParticipantRepository.findByProjectIdWithUserAndProject(
                projectId);
        ProjectProgressSnapshot progressSnapshot = projectProgressCalculator.buildProjectProgressSnapshot(
                projectId,
                projectParticipants,
                datasetItemSummaries);

        List<ProjectDetailParticipantDto> participants = projectParticipants
                .stream()
                .map(projectParticipant -> toDetailParticipantDto(
                        projectParticipant,
                        progressSnapshot.completionPercentageForUser(projectParticipant.getUser().getId())))
                .toList();

        return new ProjectDetailDto(
                project.getId(),
                project.getResearchGroup().getId(),
                project.getResearchGroup().getName(),
                project.getName(),
                project.getDescription(),
                project.getProjectType(),
                progressSnapshot.projectCompletionPercentage(),
                participant.getRole(),
                participants,
                datasetItemDtos,
                labels,
                guidelineText,
                guidelinePdfBase64,
                guidelinePdfAvailable,
                guidelinePdfAvailable ? "application/pdf" : null,
                guidelinePdfSizeBytes,
                project.getAnnotationTargetColumn(),
                datasetItemSummaries.size(),
                canManageProject,
                canManageProject,
                canManageProject,
                project.isArchived(),
                project.getCreatedAt());
    }

    @Override
    @Transactional
    public ProjectSummaryDto createProject(String authenticatedEmail, Long researchGroupId,
            CreateProjectRequestDto request) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        researchGroupApiService.findGroupById(researchGroupId)
                .orElseThrow(() -> new AccessDeniedException(ProjectConstants.NOT_MEMBER_ERROR));

        ResearchGroupMemberInfo requesterMembership = researchGroupApiService
                .findActiveMember(researchGroupId, userInfo.userId())
                .orElseThrow(() -> new AccessDeniedException(ProjectConstants.NOT_MEMBER_ERROR));

        if (!requesterMembership.isOwnerOrAdmin()) {
            throw new AccessDeniedException("Only owners or admins can create projects");
        }

        Project project = new Project();
        entityReferenceService.attachResearchGroup(project, researchGroupId);
        project.setName(request.name().trim());
        project.setDescription(StringUtils.trimToNull(request.description()));

        project = projectRepository.save(project);

        ProjectParticipant creatorParticipant = new ProjectParticipant();
        creatorParticipant.setProject(project);
        entityReferenceService.attachUser(creatorParticipant, userInfo.userId());
        creatorParticipant.setRole(ProjectParticipantRole.CREATOR);
        creatorParticipant.setIaaGroup(ProjectParticipantIaaGroup.GROUP_A);
        projectParticipantRepository.save(creatorParticipant);

        return toProjectSummaryDto(project, researchGroupId);
    }

    @Override
    @Transactional
    public ProjectDetailDto updateProject(String authenticatedEmail, Long researchGroupId, Long projectId,
            UpdateProjectRequestDto request) {
        UserInfo requesterInfo = authApiService.findUserByEmail(authenticatedEmail);

        Project project = projectRepository.findByIdAndResearchGroupId(projectId, researchGroupId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        ProjectParticipant requesterParticipant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, requesterInfo.userId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (requesterParticipant.getRole() != ProjectParticipantRole.CREATOR) {
            throw new AccessDeniedException("Only project creators can edit projects");
        }

        project.setName(request.name().trim());
        project.setDescription(StringUtils.trimToNull(request.description()));
        projectRepository.save(project);

        if (request.participantAssignments() != null && !request.participantAssignments().isEmpty()) {
            projectParticipantService.replaceProjectParticipantAssignments(
                    projectId, requesterInfo.userId(), researchGroupId, request.participantAssignments());
        } else {
            projectParticipantService.replaceProjectParticipants(
                    projectId, requesterInfo.userId(), researchGroupId, request.participantUserIds());
        }

        return getAssignedProjectDetail(authenticatedEmail, projectId);
    }

    @Override
    @Transactional
    public void deleteProject(
            String authenticatedEmail,
            Long researchGroupId,
            Long projectId) {
        UserInfo requesterInfo = authApiService.findUserByEmail(authenticatedEmail);

        Project project = projectRepository.findByIdAndResearchGroupId(projectId, researchGroupId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        ProjectParticipant requesterParticipant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, requesterInfo.userId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (requesterParticipant.getRole() != ProjectParticipantRole.CREATOR) {
            throw new AccessDeniedException("Only project creators can delete projects");
        }

        deleteProjectData(projectId, project);
    }

    @Override
    @Transactional
    public void cleanupIncompleteProject(String authenticatedEmail, Long researchGroupId, Long projectId) {
        UserInfo requesterInfo = authApiService.findUserByEmail(authenticatedEmail);

        Project project = projectRepository.findByIdAndResearchGroupId(projectId, researchGroupId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        ProjectParticipant requesterParticipant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, requesterInfo.userId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (requesterParticipant.getRole() != ProjectParticipantRole.CREATOR) {
            throw new AccessDeniedException("Only project creators can cleanup project creation failures");
        }

        if (!isEligibleForWizardCleanup(project)) {
            throw new AccessDeniedException("Project is not eligible for wizard cleanup");
        }

        deleteProjectData(projectId, project);
    }

    @Override
    @Transactional
    public ProjectDetailDto archiveProject(String authenticatedEmail, Long projectId) {
        return updateProjectArchiveState(authenticatedEmail, projectId, true);
    }

    @Override
    @Transactional
    public ProjectDetailDto unarchiveProject(String authenticatedEmail, Long projectId) {
        return updateProjectArchiveState(authenticatedEmail, projectId, false);
    }

    private boolean isEligibleForWizardCleanup(Project project) {
        Long projectId = project.getId();
        Instant createdAt = project.getCreatedAt();
        if (createdAt == null || createdAt.isBefore(Instant.now().minus(wizardCleanupWindow))) {
            return false;
        }

        if (annotationRepository.countByDatasetItemProjectId(projectId) > 0) {
            return false;
        }

        return projectParticipantRepository.countByProjectIdAndRole(
                projectId,
                ProjectParticipantRole.PARTICIPANT) == 0;
    }

    private DatasetItemDto toDatasetItemDto(DatasetItemSummaryProjection item) {
        return new DatasetItemDto(
                item.getId(),
                item.getItemIndex(),
                item.getFileName(),
                item.getMimeType(),
                item.getSizeBytes() == null ? 0L : item.getSizeBytes(),
                item.getCreatedAt());
    }

    private void deleteProjectData(Long projectId, Project project) {
        notificationService.deleteNotificationsByProjectId(projectId);
        annotationRepository.deleteByDatasetItemProjectId(projectId);
        datasetItemRepository.deleteByProjectId(projectId);
        projectParticipantRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
    }

    private ProjectSummaryDto toProjectSummaryDto(Project project, Long researchGroupId) {
        return new ProjectSummaryDto(
                project.getId(),
                researchGroupId,
                project.getName(),
                project.getDescription(),
                project.getCreatedAt());
    }

    private ProjectAssignedSummaryDto toAssignedSummaryDto(ProjectParticipant participant) {
        Project project = participant.getProject();
        int completionPercentage = calculateCompletionPercentage(project.getId());

        return new ProjectAssignedSummaryDto(
                project.getId(),
                project.getResearchGroup().getId(),
                project.getResearchGroup().getName(),
                project.getName(),
                project.getDescription(),
                completionPercentage,
                participant.getRole(),
                project.isArchived(),
                project.getCreatedAt());
    }

    private ProjectDetailDto updateProjectArchiveState(String authenticatedEmail, Long projectId, boolean archived) {
        UserInfo requesterInfo = authApiService.findUserByEmail(authenticatedEmail);

        ProjectParticipant requesterParticipant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, requesterInfo.userId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (requesterParticipant.getRole() != ProjectParticipantRole.CREATOR) {
            throw new AccessDeniedException("Only project creators can archive projects");
        }

        Project project = requesterParticipant.getProject();
        project.setArchived(archived);
        projectRepository.save(project);

        return getAssignedProjectDetail(authenticatedEmail, projectId);
    }

    private int sanitizePage(int page) {
        return Math.max(0, page);
    }

    private int sanitizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PROJECT_PAGE_SIZE;
        }

        return Math.min(size, MAX_PROJECT_PAGE_SIZE);
    }

    private ProjectDetailParticipantDto toDetailParticipantDto(ProjectParticipant participant,
            int completionPercentage) {
        var participantUser = participant.getUser();

        return new ProjectDetailParticipantDto(
                participantUser.getId(),
                participantUser.getFirstName(),
                participantUser.getLastName(),
                participantUser.getEmail(),
                participant.getRole(),
                participant.getIaaGroup(),
                completionPercentage);
    }

    private int calculateCompletionPercentage(Long projectId) {
        return projectMetricsCacheService.getProjectCompletionPercentage(projectId);
    }

}
