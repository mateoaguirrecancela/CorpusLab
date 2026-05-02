package es.udc.fic.corpuslab.modules.project.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.DatasetItemDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAssignedSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectDetailDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectDetailParticipantDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupResponseDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.UpdateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.Guideline;
import es.udc.fic.corpuslab.modules.project.entities.Label;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
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
import java.util.LinkedHashMap;
import jakarta.persistence.EntityManager;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final AnnotationRepository annotationRepository;
    private final AuthApiService authApiService;
    private final ResearchGroupApiService researchGroupApiService;
    private final NotificationService notificationService;
    private final ProjectParticipantService projectParticipantService;
    private final EntityManager entityManager;

    public ProjectServiceImpl(
            ProjectRepository projectRepository,
            ProjectParticipantRepository projectParticipantRepository,
            DatasetItemRepository datasetItemRepository,
            AnnotationRepository annotationRepository,
            AuthApiService authApiService,
            ResearchGroupApiService researchGroupApiService,
            NotificationService notificationService,
            ProjectParticipantService projectParticipantService,
            EntityManager entityManager) {
        this.projectRepository = projectRepository;
        this.projectParticipantRepository = projectParticipantRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.annotationRepository = annotationRepository;
        this.authApiService = authApiService;
        this.researchGroupApiService = researchGroupApiService;
        this.notificationService = notificationService;
        this.projectParticipantService = projectParticipantService;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectAssignedSummaryDto> findAssignedProjectsByResearchGroup(String authenticatedEmail,
            Long researchGroupId) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        researchGroupApiService
                .findActiveMember(researchGroupId, userInfo.userId())
                .orElseThrow(() -> new AccessDeniedException(ProjectConstants.NOT_MEMBER_ERROR));

        return projectParticipantRepository
                .findByProjectResearchGroupIdAndUserIdOrderByProjectCreatedAtDesc(researchGroupId, userInfo.userId())
                .stream()
                .map(this::toAssignedSummaryDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectAssignedSummaryDto> findMyAssignedProjects(String authenticatedEmail) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        return projectParticipantRepository.findByUserIdOrderByProjectCreatedAtDesc(userInfo.userId())
                .stream()
                .map(this::toAssignedSummaryDto)
                .toList();
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

        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);
        List<DatasetItemDto> datasetItemDtos = datasetItems.stream().map(this::toDatasetItemDto).toList();
        List<ProjectParticipant> projectParticipants = projectParticipantRepository
                .findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(projectId);
        Map<Long, Map<Long, Map<Integer, Object>>> annotationLookup = buildAnnotationLookup(projectId);
        ProjectAnnotationUtils.ProjectProgressSnapshot progressSnapshot = ProjectAnnotationUtils
                .buildProjectProgressSnapshot(
                        projectParticipants,
                        datasetItems,
                        annotationLookup);

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
                datasetItems.size(),
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

        projectParticipantService.replaceProjectParticipants(
                projectId, requesterInfo.userId(), researchGroupId, request.participantUserIds());

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

        validateSetupRequest(
                project.getId(),
                request.projectType(),
                normalizedLabels,
                request.guidelineText(),
                request.guidelinePdfBase64(),
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
        String guidelinePdfBase64 = StringUtils.trimToNull(request.guidelinePdfBase64());
        guideline.setContent(guidelineText);
        guideline.setFileUrl(guidelinePdfBase64);

        projectRepository.save(project);

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

    private DatasetItemDto toDatasetItemDto(DatasetItem item) {
        String fileName = ProjectDatasetUtils
                .valueAsString(item.getContent().get(ProjectConstants.CONTENT_KEY_FILE_NAME));
        String mimeType = ProjectDatasetUtils
                .valueAsString(item.getContent().get(ProjectConstants.CONTENT_KEY_MIME_TYPE));
        long sizeBytes = valueAsLong(item.getContent().get(ProjectConstants.CONTENT_KEY_SIZE_BYTES));

        return new DatasetItemDto(
                item.getId(),
                item.getItemIndex(),
                fileName,
                mimeType,
                sizeBytes,
                item.getCreatedAt());
    }

    private long valueAsLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return value == null ? 0L : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
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
                project.getCreatedAt());
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
                completionPercentage);
    }

    private int calculateCompletionPercentage(Long projectId) {
        return ProjectAnnotationUtils.buildProjectProgressSnapshot(
                projectParticipantRepository.findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(projectId),
                datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId),
                buildAnnotationLookup(projectId))
                .projectCompletionPercentage();
    }

    private Map<Long, Map<Long, Map<Integer, Object>>> buildAnnotationLookup(Long projectId) {
        List<Annotation> annotations = annotationRepository.findByDatasetItemProjectId(projectId);

        Map<Long, Map<Long, Map<Integer, Object>>> annotationLookup = new LinkedHashMap<>();
        for (Annotation annotation : annotations) {
            if (annotation.getDatasetItem() == null || annotation.getUser() == null) {
                continue;
            }

            Long datasetItemId = annotation.getDatasetItem().getId();
            Long userId = annotation.getUser().getId();
            Integer stepIndex = annotation.getStepIndex();

            if (datasetItemId == null || userId == null || stepIndex == null) {
                continue;
            }

            annotationLookup
                    .computeIfAbsent(datasetItemId, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(userId, ignored -> new LinkedHashMap<>())
                    .put(stepIndex, annotation.getPayload());
        }

        return annotationLookup;
    }

}