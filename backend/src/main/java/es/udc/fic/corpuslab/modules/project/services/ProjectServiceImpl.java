package es.udc.fic.corpuslab.modules.project.services;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import es.udc.fic.corpuslab.modules.project.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.DatasetItemDto;
import es.udc.fic.corpuslab.modules.project.dtos.DatasetItemSummaryProjection;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAssignedSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectDetailDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectDetailParticipantDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupResponseDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.UpdateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.UserAnnotationCountDto;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.Guideline;
import es.udc.fic.corpuslab.modules.project.entities.Label;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantIaaGroup;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.exceptions.InvalidProjectDatasetException;
import es.udc.fic.corpuslab.modules.project.exceptions.InvalidProjectSetupException;
import es.udc.fic.corpuslab.modules.project.exceptions.ProjectNotFoundException;
import es.udc.fic.corpuslab.modules.project.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.api.ResearchGroupApiService;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupInfo;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupMemberInfo;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupNotFoundException;

import es.udc.fic.corpuslab.modules.project.utils.ProjectConstants;
import es.udc.fic.corpuslab.modules.project.utils.ProjectDatasetUtils;
import es.udc.fic.corpuslab.modules.project.utils.ProjectAnnotationUtils;
import es.udc.fic.corpuslab.modules.project.utils.ProjectCommonUtils;
import jakarta.persistence.EntityManager;

@Service
public class ProjectServiceImpl implements ProjectService {

    private static final int DEFAULT_PROJECT_PAGE_SIZE = 12;
    private static final int MAX_PROJECT_PAGE_SIZE = 50;
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final ProjectRepository projectRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final AnnotationRepository annotationRepository;
    private final AuthApiService authApiService;
    private final ResearchGroupApiService researchGroupApiService;
    private final NotificationService notificationService;
    private final ProjectParticipantService projectParticipantService;
    private final ProjectMetricsCacheService projectMetricsCacheService;
    private final EntityManager entityManager;
    private final long maxGuidelinePdfSizeBytes;
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
            EntityManager entityManager,
            @Value("${app.guideline.max-pdf-size-bytes:10485760}") long maxGuidelinePdfSizeBytes,
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
        this.entityManager = entityManager;
        this.maxGuidelinePdfSizeBytes = Math.max(1L, maxGuidelinePdfSizeBytes);
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

        List<DatasetItemSummaryProjection> datasetItemSummaries = datasetItemRepository
                .findSummariesByProjectId(projectId);
        List<DatasetItemDto> datasetItemDtos = datasetItemSummaries.stream()
                .map(this::toDatasetItemDto)
                .toList();
        List<ProjectParticipant> projectParticipants = projectParticipantRepository.findByProjectIdWithUserAndProject(
                projectId);
        ProjectAnnotationUtils.ProjectProgressSnapshot progressSnapshot = buildProjectProgressSnapshot(
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
                project.getAnnotationTargetColumn(),
                datasetItemSummaries.size(),
                project.isArchived(),
                project.getCreatedAt());
    }

    @Override
    @Transactional
    public ProjectSummaryDto createProject(String authenticatedEmail, Long researchGroupId,
            CreateProjectRequestDto request) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        ResearchGroupInfo groupInfo = researchGroupApiService.findGroupById(researchGroupId)
                .orElseThrow(() -> new ResearchGroupNotFoundException(researchGroupId));

        ResearchGroupMemberInfo requesterMembership = researchGroupApiService
                .findActiveMember(researchGroupId, userInfo.userId())
                .orElseThrow(() -> new AccessDeniedException(ProjectConstants.NOT_MEMBER_ERROR));

        if (!requesterMembership.isOwnerOrAdmin()) {
            throw new AccessDeniedException("Only owners or admins can create projects");
        }

        ResearchGroup researchGroupRef = entityManager.getReference(ResearchGroup.class, researchGroupId);

        Project project = new Project();
        project.setResearchGroup(researchGroupRef);
        project.setName(request.name().trim());
        project.setDescription(
                request.description() != null && !request.description().isBlank()
                        ? request.description().trim()
                        : null);

        project = projectRepository.save(project);

        ProjectParticipant creatorParticipant = new ProjectParticipant();
        creatorParticipant.setProject(project);
        creatorParticipant.setUser(entityManager.getReference(
                es.udc.fic.corpuslab.modules.auth.entities.User.class, userInfo.userId()));
        creatorParticipant.setRole(ProjectParticipantRole.CREATOR);
        creatorParticipant.setIaaGroup(ProjectParticipantIaaGroup.GROUP_A);
        projectParticipantRepository.save(creatorParticipant);

        return new ProjectSummaryDto(
                project.getId(),
                researchGroupId,
                project.getName(),
                project.getDescription(),
                project.getCreatedAt());
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
        project.setDescription(
                request.description() != null && !request.description().isBlank()
                        ? request.description().trim()
                        : null);
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

        notificationService.deleteNotificationsByProjectId(projectId);
        annotationRepository.deleteByDatasetItemProjectId(projectId);
        datasetItemRepository.deleteByProjectId(projectId);
        projectParticipantRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
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

        notificationService.deleteNotificationsByProjectId(projectId);
        annotationRepository.deleteByDatasetItemProjectId(projectId);
        datasetItemRepository.deleteByProjectId(projectId);
        projectParticipantRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
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

    @Override
    @Transactional
    public ProjectSetupResponseDto configureProjectSetup(String authenticatedEmail, Long researchGroupId,
            Long projectId, ProjectSetupRequestDto request) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        Project project = projectRepository.findByIdAndResearchGroupId(projectId, researchGroupId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        ResearchGroupMemberInfo requesterMembership = researchGroupApiService
                .findActiveMember(researchGroupId, userInfo.userId())
                .orElseThrow(() -> new AccessDeniedException(ProjectConstants.NOT_MEMBER_ERROR));

        if (!requesterMembership.isOwnerOrAdmin()) {
            throw new AccessDeniedException("Only owners or admins can configure project setup");
        }

        List<ProjectSetupLabelDto> normalizedLabels = normalizeLabels(request.labels());
        String normalizedAnnotationTargetColumn = StringUtils.trimToNull(request.annotationTargetColumn());

        String normalizedGuidelinePdfBase64 = normalizeAndValidateGuidelinePdfBase64(request.guidelinePdfBase64());

        validateSetupRequest(
                project.getId(),
                request.projectType(),
                normalizedLabels,
                request.guidelineText(),
                normalizedGuidelinePdfBase64,
                normalizedAnnotationTargetColumn);

        if (request.projectType() == ProjectType.NER) {
            validateNerDatasetCompatibility(project.getId());
        }

        project.setProjectType(request.projectType());
        project.setSetupCompleted(true);
        project.setAnnotationTargetColumn(normalizedAnnotationTargetColumn);

        project.getLabels().clear();

        for (ProjectSetupLabelDto labelInput : normalizedLabels) {
            Label label = new Label();
            label.setProject(project);
            label.setName(labelInput.name());
            label.setColor(labelInput.color());
            project.getLabels().add(label);
        }

        Guideline guideline = project.getGuideline();
        if (guideline == null) {
            guideline = new Guideline();
            guideline.setProject(project);
            project.setGuideline(guideline);
        }

        String guidelineText = StringUtils.trimToNull(request.guidelineText());
        String guidelinePdfBase64 = normalizedGuidelinePdfBase64;
        guideline.setContent(guidelineText);
        guideline.setFileUrl(guidelinePdfBase64);

        projectRepository.save(project);
        projectMetricsCacheService.evictProjectReadCaches(projectId);

        return new ProjectSetupResponseDto(
                project.getId(),
                project.getProjectType(),
                normalizedLabels,
                guidelineText,
                guidelinePdfBase64,
                project.getAnnotationTargetColumn(),
                project.isSetupCompleted());
    }

    private List<ProjectSetupLabelDto> normalizeLabels(List<ProjectSetupLabelDto> labels) {
        if (labels == null || labels.isEmpty()) {
            return List.of();
        }

        List<ProjectSetupLabelDto> normalizedLabels = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (ProjectSetupLabelDto label : labels) {
            if (label != null) {
                String normalizedName = StringUtils.trimToNull(label.name());
                if (normalizedName != null) {
                    String normalizedColor = ProjectCommonUtils.normalizeHexColor(label.color());

                    String key = normalizedName.toLowerCase();
                    if (!seen.add(key)) {
                        throw new InvalidProjectSetupException("Duplicated labels are not allowed");
                    }

                    normalizedLabels.add(new ProjectSetupLabelDto(normalizedName, normalizedColor));
                }
            }
        }

        return normalizedLabels;
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

    private String normalizeAndValidateGuidelinePdfBase64(String rawGuidelinePdfBase64) {
        String normalizedInput = StringUtils.trimToNull(rawGuidelinePdfBase64);
        if (normalizedInput == null) {
            return null;
        }

        String normalizedBase64 = ProjectDatasetUtils.normalizeStoredBase64(normalizedInput);
        long maxBase64Length = ((maxGuidelinePdfSizeBytes + 2L) / 3L) * 4L + 128L;
        if (normalizedBase64.length() > maxBase64Length) {
            throw new InvalidProjectSetupException("Guideline PDF exceeds the maximum allowed size");
        }

        byte[] pdfBytes;
        try {
            pdfBytes = ProjectDatasetUtils.decodeStoredBase64(normalizedBase64);
        } catch (IllegalArgumentException ex) {
            throw new InvalidProjectSetupException("Guideline PDF content is not valid Base64");
        }

        if (pdfBytes.length == 0 || pdfBytes.length > maxGuidelinePdfSizeBytes) {
            throw new InvalidProjectSetupException("Guideline PDF exceeds the maximum allowed size");
        }

        if (!hasPdfMagicHeader(pdfBytes)) {
            throw new InvalidProjectSetupException("Guideline file must be a valid PDF");
        }

        return normalizedBase64;
    }

    private boolean hasPdfMagicHeader(byte[] bytes) {
        if (bytes.length < PDF_MAGIC.length) {
            return false;
        }

        for (int index = 0; index < PDF_MAGIC.length; index++) {
            if (bytes[index] != PDF_MAGIC[index]) {
                return false;
            }
        }
        return true;
    }

    private void validateSetupRequest(
            Long projectId,
            ProjectType projectType,
            List<ProjectSetupLabelDto> labels,
            String guidelineText,
            String guidelinePdfBase64,
            String annotationTargetColumn) {
        if (projectType == ProjectType.SEQ2SEQ && !labels.isEmpty()) {
            throw new InvalidProjectSetupException("Seq2Seq projects do not allow labels");
        }

        if (projectType != ProjectType.SEQ2SEQ && labels.isEmpty()) {
            throw new InvalidProjectSetupException("At least one label is required for this project type");
        }

        if ((projectType == ProjectType.TEXT_CLASSIFICATION_SIMPLE
                || projectType == ProjectType.TEXT_CLASSIFICATION_MULTILABEL) && labels.size() < 2) {
            throw new InvalidProjectSetupException("Classification projects require at least 2 labels");
        }

        if (projectType == ProjectType.NER && labels.stream().anyMatch(label -> label.color() == null)) {
            throw new InvalidProjectSetupException("NER labels require a color");
        }

        String normalizedGuidelineText = StringUtils.trimToNull(guidelineText);
        String normalizedGuidelinePdf = StringUtils.trimToNull(guidelinePdfBase64);

        if (normalizedGuidelineText == null && normalizedGuidelinePdf == null) {
            throw new InvalidProjectSetupException("Provide either a guideline text or a guideline PDF");
        }

        if (normalizedGuidelineText != null && normalizedGuidelinePdf != null) {
            throw new InvalidProjectSetupException("Guideline text and guideline PDF are mutually exclusive");
        }

        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);
        boolean hasCsvDataset = datasetItems.stream().anyMatch(ProjectDatasetUtils::isCsvDatasetItem);

        if (hasCsvDataset) {
            if (annotationTargetColumn == null) {
                throw new InvalidProjectSetupException(
                        "Annotation target column is required when the dataset includes CSV files");
            }

            validateAnnotationTargetColumnForCsvDatasetItems(datasetItems, annotationTargetColumn);
        } else if (annotationTargetColumn != null) {
            throw new InvalidProjectSetupException(
                    "Annotation target column can only be configured when the dataset includes CSV files");
        }
    }

    private void validateAnnotationTargetColumnForCsvDatasetItems(
            List<DatasetItem> datasetItems,
            String annotationTargetColumn) {
        for (DatasetItem datasetItem : datasetItems) {
            if (!ProjectDatasetUtils.isCsvDatasetItem(datasetItem)) {
                continue;
            }

            List<String> headerColumns = ProjectDatasetUtils.parseCsvHeaderColumns(datasetItem);
            boolean columnExists = headerColumns.stream()
                    .anyMatch(headerColumn -> headerColumn.equalsIgnoreCase(annotationTargetColumn));

            if (!columnExists) {
                throw new InvalidProjectSetupException(
                        "Selected annotation target column '" + annotationTargetColumn
                                + "' was not found in CSV file "
                                + ProjectDatasetUtils.describeDatasetItem(datasetItem));
            }
        }
    }

    private void validateNerDatasetCompatibility(Long projectId) {
        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);

        if (datasetItems.isEmpty()) {
            throw new InvalidProjectSetupException(
                    "NER projects require at least one dataset file in text, JSON or CSV format");
        }

        List<String> unsupportedFiles = datasetItems.stream()
                .filter(datasetItem -> !isNerCompatibleDatasetItem(datasetItem))
                .map(ProjectDatasetUtils::describeDatasetItem)
                .limit(5)
                .toList();

        if (!unsupportedFiles.isEmpty()) {
            throw new InvalidProjectSetupException(
                    "NER projects only support text, JSON or CSV files. Unsupported files: "
                            + String.join(", ", unsupportedFiles));
        }
    }

    private boolean isNerCompatibleDatasetItem(DatasetItem datasetItem) {
        String mimeType = ProjectDatasetUtils
                .valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_MIME_TYPE)).trim()
                .toLowerCase();
        String extension = ProjectDatasetUtils.extractFileExtension(
                ProjectDatasetUtils.valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_FILE_NAME)));

        if (mimeType.equals("text/plain")
                || mimeType.equals("application/json")
                || mimeType.equals("text/json")
                || mimeType.endsWith("+json")
                || mimeType.contains("csv")) {
            return true;
        }

        return extension.equals("txt") || extension.equals("json") || extension.equals("csv");
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

    private ProjectAnnotationUtils.ProjectProgressSnapshot buildProjectProgressSnapshot(
            Long projectId,
            List<ProjectParticipant> participants,
            List<DatasetItemSummaryProjection> datasetItemSummaries) {
        long totalSteps = resolveTotalSteps(datasetItemSummaries);
        Map<Long, Long> completedStepCounts = buildCompletedStepCountMap(projectId);
        Map<Long, Long> completedStepsByUser = new LinkedHashMap<>();
        Map<Long, Integer> completionPercentageByUser = new LinkedHashMap<>();

        for (ProjectParticipant participant : participants) {
            if (participant.getUser() == null || participant.getUser().getId() == null) {
                continue;
            }
            Long userId = participant.getUser().getId();
            long completedSteps = Math.max(0L, Math.min(
                    completedStepCounts.getOrDefault(userId, 0L),
                    totalSteps));
            completedStepsByUser.put(userId, completedSteps);
            completionPercentageByUser.put(userId, ProjectAnnotationUtils.toPercentage(completedSteps, totalSteps));
        }

        long totalCompletedSteps = completedStepsByUser.values().stream()
                .mapToLong(Long::longValue)
                .sum();
        long totalPossibleSteps = totalSteps * completedStepsByUser.size();

        return new ProjectAnnotationUtils.ProjectProgressSnapshot(
                totalSteps,
                ProjectAnnotationUtils.toPercentage(totalCompletedSteps, totalPossibleSteps),
                completedStepsByUser,
                completionPercentageByUser);
    }

    private long resolveTotalSteps(List<DatasetItemSummaryProjection> datasetItemSummaries) {
        return datasetItemSummaries.stream()
                .map(DatasetItemSummaryProjection::getStepCount)
                .mapToLong(Long::longValue)
                .sum();
    }

    private Map<Long, Long> buildCompletedStepCountMap(Long projectId) {
        List<UserAnnotationCountDto> counts = annotationRepository.countCompletedStepsByUser(projectId);
        Map<Long, Long> result = new LinkedHashMap<>();
        for (UserAnnotationCountDto count : counts) {
            if (count.userId() != null) {
                result.put(count.userId(), count.completedSteps());
            }
        }
        return result;
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
        es.udc.fic.corpuslab.modules.auth.entities.User participantUser = participant.getUser();

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
