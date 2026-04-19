package es.udc.fic.corpuslab.modules.project.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.auth.utils.EmailNormalizer;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.DatasetItemDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationSourceContentDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationStepDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationWorkspaceDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAssignedSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectDetailDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectDetailParticipantDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupResponseDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.SaveProjectAnnotationStepRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.SaveProjectAnnotationStepResponseDto;
import es.udc.fic.corpuslab.modules.project.dtos.UploadProjectDatasetResponseDto;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.Guideline;
import es.udc.fic.corpuslab.modules.project.entities.Label;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.exceptions.InvalidProjectDatasetException;
import es.udc.fic.corpuslab.modules.project.exceptions.InvalidProjectParticipantsException;
import es.udc.fic.corpuslab.modules.project.exceptions.InvalidProjectSetupException;
import es.udc.fic.corpuslab.modules.project.exceptions.ProjectNotFoundException;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.dtos.ResearchGroupMemberDto;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupMember;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;

@Service
public class ProjectServiceImpl implements ProjectService {

    private static final String NOT_MEMBER_ERROR = "User is not a member of this research group";
    private static final String CONTENT_KEY_FILE_NAME = "fileName";
    private static final String CONTENT_KEY_MIME_TYPE = "mimeType";
    private static final String CONTENT_KEY_SIZE_BYTES = "sizeBytes";
    private static final String CONTENT_KEY_BASE64 = "base64";
    private static final String CONTENT_KEY_ANNOTATIONS_BY_USER = "annotationsByUser";
    private static final String USER_ANNOTATION_STEPS_KEY = "steps";
    private static final String ANNOTATION_KEY_NOTES = "notes";
    private static final String NER_ANNOTATION_KEY_ENTITIES = "entities";
    private static final String NER_ANNOTATION_KEY_LABEL = "label";
    private static final String NER_ANNOTATION_KEY_TEXT = "text";
    private static final String NER_ANNOTATION_KEY_START_OFFSET = "startOffset";
    private static final String NER_ANNOTATION_KEY_END_OFFSET = "endOffset";
    private static final int DEFAULT_ANNOTATION_STEPS_LIMIT = 50;
    private static final int MAX_ANNOTATION_STEPS_LIMIT = 250;
    private static final int PREVIEW_MAX_LENGTH = 160;

    private final UserRepository userRepository;
    private final ResearchGroupRepository researchGroupRepository;
    private final ResearchGroupMemberRepository researchGroupMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final NotificationService notificationService;

    public ProjectServiceImpl(
            UserRepository userRepository,
            ResearchGroupRepository researchGroupRepository,
            ResearchGroupMemberRepository researchGroupMemberRepository,
            ProjectRepository projectRepository,
            ProjectParticipantRepository projectParticipantRepository,
            DatasetItemRepository datasetItemRepository,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.researchGroupRepository = researchGroupRepository;
        this.researchGroupMemberRepository = researchGroupMemberRepository;
        this.projectRepository = projectRepository;
        this.projectParticipantRepository = projectParticipantRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectAssignedSummaryDto> findAssignedProjectsByResearchGroup(
            String authenticatedEmail,
            Long researchGroupId) {
        User user = findUserByEmail(authenticatedEmail);

        researchGroupMemberRepository
                .findActiveMemberByGroupIdAndUserId(researchGroupId, user.getId())
                .orElseThrow(() -> new AccessDeniedException(NOT_MEMBER_ERROR));

        return projectParticipantRepository
                .findByProjectResearchGroupIdAndUserIdOrderByProjectCreatedAtDesc(researchGroupId, user.getId())
                .stream()
                .map(this::toAssignedSummaryDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectAssignedSummaryDto> findMyAssignedProjects(String authenticatedEmail) {
        User user = findUserByEmail(authenticatedEmail);

        return projectParticipantRepository.findByUserIdOrderByProjectCreatedAtDesc(user.getId())
                .stream()
                .map(this::toAssignedSummaryDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDetailDto getAssignedProjectDetail(String authenticatedEmail, Long projectId) {
        User user = findUserByEmail(authenticatedEmail);

        ProjectParticipant participant = projectParticipantRepository.findByProjectIdAndUserId(projectId, user.getId())
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
        ProjectProgressSnapshot progressSnapshot = buildProjectProgressSnapshot(projectParticipants, datasetItems);

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
                datasetItems.size(),
                project.getCreatedAt());
    }

    @Override
    @Transactional
    public ProjectSummaryDto createProject(String authenticatedEmail, Long researchGroupId,
            CreateProjectRequestDto request) {
        String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));

        ResearchGroup researchGroup = researchGroupRepository.findById(researchGroupId)
                .orElseThrow(() -> new ResearchGroupNotFoundException(researchGroupId));

        ResearchGroupMember requesterMembership = researchGroupMemberRepository
                .findActiveMemberByGroupIdAndUserId(researchGroupId, user.getId())
                .orElseThrow(() -> new AccessDeniedException(NOT_MEMBER_ERROR));

        if (requesterMembership.getRole() != ResearchGroupMemberRole.OWNER
                && requesterMembership.getRole() != ResearchGroupMemberRole.ADMIN) {
            throw new AccessDeniedException("Only owners or admins can create projects");
        }

        Project project = new Project();
        project.setResearchGroup(researchGroup);
        project.setName(request.name().trim());
        project.setDescription(
                request.description() != null && !request.description().isBlank()
                        ? request.description().trim()
                        : null);

        project = projectRepository.save(project);

        ProjectParticipant creatorParticipant = new ProjectParticipant();
        creatorParticipant.setProject(project);
        creatorParticipant.setUser(user);
        creatorParticipant.setRole(ProjectParticipantRole.CREATOR);
        projectParticipantRepository.save(creatorParticipant);

        return new ProjectSummaryDto(
                project.getId(),
                researchGroup.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt());
    }

    @Override
    @Transactional
    public UploadProjectDatasetResponseDto uploadDataset(
            String authenticatedEmail,
            Long researchGroupId,
            Long projectId,
            List<MultipartFile> files) {
        User user = findUserByEmail(authenticatedEmail);

        Project project = projectRepository.findByIdAndResearchGroupId(projectId, researchGroupId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        ResearchGroupMember requesterMembership = researchGroupMemberRepository
                .findActiveMemberByGroupIdAndUserId(researchGroupId, user.getId())
                .orElseThrow(() -> new AccessDeniedException(NOT_MEMBER_ERROR));

        if (requesterMembership.getRole() != ResearchGroupMemberRole.OWNER
                && requesterMembership.getRole() != ResearchGroupMemberRole.ADMIN) {
            throw new AccessDeniedException("Only owners or admins can upload datasets");
        }

        if (files == null || files.isEmpty()) {
            throw new InvalidProjectDatasetException("At least one file is required to upload a dataset");
        }

        int nextIndex = (int) datasetItemRepository.countByProjectId(projectId);
        List<DatasetItem> createdItems = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new InvalidProjectDatasetException("All dataset files must have content");
            }

            try {
                Map<String, Object> content = new LinkedHashMap<>();
                content.put("fileName", file.getOriginalFilename());
                content.put("mimeType",
                        file.getContentType() != null ? file.getContentType() : "application/octet-stream");
                content.put("sizeBytes", file.getSize());
                content.put("base64", Base64.getEncoder().encodeToString(file.getBytes()));
                content.put("uploadedAt", Instant.now().toString());

                DatasetItem item = new DatasetItem();
                item.setProject(project);
                item.setItemIndex(nextIndex++);
                item.setContent(content);
                createdItems.add(item);
            } catch (IOException ex) {
                throw new InvalidProjectDatasetException("Could not read one of the uploaded files");
            }
        }

        List<DatasetItem> savedItems = datasetItemRepository.saveAll(createdItems);
        List<DatasetItemDto> itemDtos = savedItems.stream().map(this::toDatasetItemDto).toList();

        return new UploadProjectDatasetResponseDto(projectId, itemDtos.size(), itemDtos);
    }

    @Override
    @Transactional
    public void assignParticipants(
            String authenticatedEmail,
            Long researchGroupId,
            Long projectId,
            List<Long> participantUserIds) {
        User requester = findUserByEmail(authenticatedEmail);

        Project project = projectRepository.findByIdAndResearchGroupId(projectId, researchGroupId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        ResearchGroupMember requesterMembership = researchGroupMemberRepository
                .findActiveMemberByGroupIdAndUserId(researchGroupId, requester.getId())
                .orElseThrow(() -> new AccessDeniedException(NOT_MEMBER_ERROR));

        if (requesterMembership.getRole() != ResearchGroupMemberRole.OWNER
                && requesterMembership.getRole() != ResearchGroupMemberRole.ADMIN) {
            throw new AccessDeniedException("Only owners or admins can assign investigators");
        }

        List<Long> requestedIds = participantUserIds == null
                ? List.of()
                : participantUserIds.stream()
                        .filter(id -> id != null && id > 0)
                        .distinct()
                        .toList();

        Set<Long> activeMemberIds = new HashSet<>(
                researchGroupMemberRepository.findMembersByGroupId(researchGroupId)
                        .stream()
                        .map(ResearchGroupMemberDto::userId)
                        .toList());

        boolean hasIdsOutsideGroup = requestedIds.stream().anyMatch(id -> !activeMemberIds.contains(id));
        if (hasIdsOutsideGroup) {
            throw new InvalidProjectParticipantsException(
                    "All selected investigators must be active members of the research group");
        }

        Long requesterId = requester.getId();
        List<Long> filteredIds = requestedIds.stream()
                .filter(id -> !id.equals(requesterId))
                .toList();

        Set<Long> existingParticipantIds = projectParticipantRepository
                .findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(projectId)
                .stream()
                .filter(projectParticipant -> projectParticipant.getRole() == ProjectParticipantRole.PARTICIPANT)
                .map(projectParticipant -> projectParticipant.getUser().getId())
                .collect(Collectors.toSet());

        projectParticipantRepository.deleteByProjectIdAndRole(projectId, ProjectParticipantRole.PARTICIPANT);

        if (filteredIds.isEmpty()) {
            return;
        }

        List<User> usersToAssign = userRepository.findAllById(filteredIds);
        if (usersToAssign.size() != filteredIds.size()) {
            throw new InvalidProjectParticipantsException("Some selected investigators do not exist");
        }

        List<ProjectParticipant> participants = usersToAssign.stream().map(user -> {
            ProjectParticipant participant = new ProjectParticipant();
            participant.setProject(project);
            participant.setUser(user);
            participant.setRole(ProjectParticipantRole.PARTICIPANT);
            return participant;
        }).toList();

        projectParticipantRepository.saveAll(participants);

        Set<Long> newParticipantIds = filteredIds.stream()
                .filter(id -> !existingParticipantIds.contains(id))
                .collect(Collectors.toSet());

        if (newParticipantIds.isEmpty()) {
            return;
        }

        Map<Long, User> usersById = usersToAssign.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        for (Long newParticipantId : newParticipantIds) {
            User recipient = usersById.get(newParticipantId);
            if (recipient != null) {
                notificationService.createProjectParticipantAssignedNotification(recipient, requester, project);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectAnnotationWorkspaceDto getAnnotationWorkspace(
            String authenticatedEmail,
            Long projectId,
            int offset,
            int limit) {
        User user = findUserByEmail(authenticatedEmail);

        ProjectParticipant participant = projectParticipantRepository.findByProjectIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        return buildAnnotationWorkspace(participant.getProject(), user.getId(), offset, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectAnnotationWorkspaceDto getParticipantAnnotationWorkspaceForCreator(
            String authenticatedEmail,
            Long projectId,
            Long participantUserId,
            int offset,
            int limit) {
        User requester = findUserByEmail(authenticatedEmail);

        ProjectParticipant requesterParticipant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, requester.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (requesterParticipant.getRole() != ProjectParticipantRole.CREATOR) {
            throw new AccessDeniedException("Only project creators can view investigators annotations");
        }

        ProjectParticipant targetParticipant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, participantUserId)
                .orElseThrow(() -> new InvalidProjectParticipantsException(
                        "Selected investigator is not assigned to this project"));

        if (targetParticipant.getRole() != ProjectParticipantRole.PARTICIPANT) {
            throw new InvalidProjectParticipantsException("Annotations can only be viewed for investigators");
        }

        return buildAnnotationWorkspace(requesterParticipant.getProject(), targetParticipant.getUser().getId(), offset,
                limit);
    }

    private ProjectAnnotationWorkspaceDto buildAnnotationWorkspace(
            Project project,
            Long annotationUserId,
            int offset,
            int limit) {
        Long projectId = project.getId();

        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);
        List<ProjectParticipant> participants = projectParticipantRepository
                .findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(projectId);

        ProjectProgressSnapshot progressSnapshot = buildProjectProgressSnapshot(participants, datasetItems);

        int sanitizedOffset = Math.max(offset, 0);
        int sanitizedLimit = sanitizeAnnotationStepsLimit(limit);

        return new ProjectAnnotationWorkspaceDto(
                projectId,
                project.getProjectType(),
                project.getLabels().stream()
                        .map(label -> new ProjectSetupLabelDto(label.getName(), label.getColor()))
                        .toList(),
                sanitizedOffset,
                sanitizedLimit,
                progressSnapshot.totalSteps(),
                progressSnapshot.completedStepsForUser(annotationUserId),
                progressSnapshot.completionPercentageForUser(annotationUserId),
                buildAnnotationSteps(datasetItems, annotationUserId, sanitizedOffset, sanitizedLimit));
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectAnnotationSourceContentDto getAnnotationSourceContent(
            String authenticatedEmail,
            Long projectId,
            Long datasetItemId) {
        User user = findUserByEmail(authenticatedEmail);

        projectParticipantRepository.findByProjectIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        DatasetItem datasetItem = datasetItemRepository.findByIdAndProjectId(datasetItemId, projectId)
                .orElseThrow(() -> new InvalidProjectDatasetException("Dataset item does not belong to this project"));

        String base64Content = valueAsString(datasetItem.getContent().get(CONTENT_KEY_BASE64));
        if (base64Content.isBlank()) {
            throw new InvalidProjectDatasetException("Dataset item has no binary content");
        }

        byte[] bytes;
        try {
            bytes = decodeStoredBase64(base64Content);
        } catch (IllegalArgumentException ex) {
            throw new InvalidProjectDatasetException("Dataset item has invalid Base64 content");
        }

        String mimeType = valueAsString(datasetItem.getContent().get(CONTENT_KEY_MIME_TYPE));
        if (mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        return new ProjectAnnotationSourceContentDto(
                valueAsString(datasetItem.getContent().get(CONTENT_KEY_FILE_NAME)),
                mimeType,
                bytes);
    }

    @Override
    @Transactional
    public SaveProjectAnnotationStepResponseDto saveAnnotationStep(
            String authenticatedEmail,
            Long projectId,
            SaveProjectAnnotationStepRequestDto request) {
        User user = findUserByEmail(authenticatedEmail);

        ProjectParticipant participant = projectParticipantRepository.findByProjectIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        List<ProjectParticipant> participants = projectParticipantRepository
                .findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(projectId);
        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);

        DatasetItem targetItem = datasetItems.stream()
                .filter(datasetItem -> datasetItem.getId().equals(request.datasetItemId()))
                .findFirst()
                .orElseThrow(() -> new InvalidProjectDatasetException("Dataset item does not belong to this project"));

        ProjectProgressSnapshot progressBeforeSave = buildProjectProgressSnapshot(participants, datasetItems);
        int completionBeforeSave = progressBeforeSave.completionPercentageForUser(user.getId());

        Object normalizedAnnotation = normalizeAnnotationPayload(request.annotation());
        if (participant.getProject().getProjectType() == ProjectType.NER) {
            normalizedAnnotation = normalizeNerAnnotationPayload(normalizedAnnotation);
        }
        DatasetStepDefinition stepDefinition = resolveStepDefinition(targetItem);

        if (stepDefinition.totalSteps() <= 0) {
            throw new InvalidProjectDatasetException("Dataset item has no annotatable steps");
        }

        int stepIndex = resolveStepIndex(request.stepIndex(), stepDefinition.totalSteps());

        storeStepAnnotation(targetItem, user.getId(), stepIndex, normalizedAnnotation);
        datasetItemRepository.save(targetItem);

        ProjectProgressSnapshot progressAfterSave = buildProjectProgressSnapshot(participants, datasetItems);
        int completionAfterSave = progressAfterSave.completionPercentageForUser(user.getId());

        if (completionBeforeSave < 100 && completionAfterSave == 100) {
            notifyProjectOwnersOnAnnotationCompletion(participant.getProject(), participants, user);
        }

        return new SaveProjectAnnotationStepResponseDto(
                projectId,
                targetItem.getId(),
                stepIndex,
                progressAfterSave.completedStepsForUser(user.getId()),
                progressAfterSave.totalSteps(),
                completionAfterSave,
                progressAfterSave.projectCompletionPercentage());
    }

    @Override
    @Transactional
    public ProjectSetupResponseDto configureProjectSetup(
            String authenticatedEmail,
            Long researchGroupId,
            Long projectId,
            ProjectSetupRequestDto request) {
        User user = findUserByEmail(authenticatedEmail);

        Project project = projectRepository.findByIdAndResearchGroupId(projectId, researchGroupId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        ResearchGroupMember requesterMembership = researchGroupMemberRepository
                .findActiveMemberByGroupIdAndUserId(researchGroupId, user.getId())
                .orElseThrow(() -> new AccessDeniedException(NOT_MEMBER_ERROR));

        if (requesterMembership.getRole() != ResearchGroupMemberRole.OWNER
                && requesterMembership.getRole() != ResearchGroupMemberRole.ADMIN) {
            throw new AccessDeniedException("Only owners or admins can configure project setup");
        }

        List<ProjectSetupLabelDto> normalizedLabels = normalizeLabels(request.labels());
        validateSetupRequest(request.projectType(), normalizedLabels, request.guidelineText(),
                request.guidelinePdfBase64());

        if (request.projectType() == ProjectType.NER) {
            validateNerDatasetCompatibility(project.getId());
        }

        project.setProjectType(request.projectType());
        project.setSetupCompleted(true);

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
                    String normalizedColor = normalizeHexColor(label.color());

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
            ProjectType projectType,
            List<ProjectSetupLabelDto> labels,
            String guidelineText,
            String guidelinePdfBase64) {
        if (projectType == ProjectType.SEQ2SEQ && !labels.isEmpty()) {
            throw new InvalidProjectSetupException("Seq2Seq projects do not allow labels");
        }

        if (projectType != ProjectType.SEQ2SEQ && labels.isEmpty()) {
            throw new InvalidProjectSetupException("At least one label is required for this project type");
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
    }

    private String normalizeHexColor(String value) {
        String trimmed = StringUtils.trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase();
    }

    private void validateNerDatasetCompatibility(Long projectId) {
        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);

        if (datasetItems.isEmpty()) {
            throw new InvalidProjectSetupException(
                    "NER projects require at least one dataset file in text or JSON format");
        }

        List<String> unsupportedFiles = datasetItems.stream()
                .filter(datasetItem -> !isNerCompatibleDatasetItem(datasetItem))
                .map(this::describeDatasetItem)
                .limit(5)
                .toList();

        if (!unsupportedFiles.isEmpty()) {
            throw new InvalidProjectSetupException(
                    "NER projects only support text or JSON files. Unsupported files: "
                            + String.join(", ", unsupportedFiles));
        }
    }

    private boolean isNerCompatibleDatasetItem(DatasetItem datasetItem) {
        String mimeType = valueAsString(datasetItem.getContent().get(CONTENT_KEY_MIME_TYPE)).trim().toLowerCase();
        String extension = extractFileExtension(valueAsString(datasetItem.getContent().get(CONTENT_KEY_FILE_NAME)));

        if (mimeType.equals("text/plain")
                || mimeType.equals("application/json")
                || mimeType.equals("text/json")
                || mimeType.endsWith("+json")) {
            return true;
        }

        return extension.equals("txt") || extension.equals("json");
    }

    private String describeDatasetItem(DatasetItem datasetItem) {
        String fileName = valueAsString(datasetItem.getContent().get(CONTENT_KEY_FILE_NAME)).trim();
        String normalizedFileName = fileName.isBlank()
                ? "dataset-item-" + datasetItem.getItemIndex()
                : fileName;
        String mimeType = valueAsString(datasetItem.getContent().get(CONTENT_KEY_MIME_TYPE)).trim();

        return mimeType.isBlank() ? normalizedFileName : normalizedFileName + " (" + mimeType + ")";
    }

    private String extractFileExtension(String fileName) {
        String normalizedFileName = fileName == null ? "" : fileName.trim().toLowerCase();
        int lastDotIndex = normalizedFileName.lastIndexOf('.');

        if (lastDotIndex < 0 || lastDotIndex == normalizedFileName.length() - 1) {
            return "";
        }

        return normalizedFileName.substring(lastDotIndex + 1);
    }

    private int sanitizeAnnotationStepsLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_ANNOTATION_STEPS_LIMIT;
        }

        return Math.min(limit, MAX_ANNOTATION_STEPS_LIMIT);
    }

    private List<ProjectAnnotationStepDto> buildAnnotationSteps(
            List<DatasetItem> datasetItems,
            Long userId,
            int offset,
            int limit) {
        List<ProjectAnnotationStepDto> steps = new ArrayList<>();
        long absoluteIndex = 0;

        for (DatasetItem datasetItem : datasetItems) {
            DatasetStepDefinition definition = resolveStepDefinition(datasetItem);
            int totalSteps = definition.totalSteps();

            if (totalSteps <= 0) {
                continue;
            }

            for (int stepIndex = 0; stepIndex < totalSteps; stepIndex++) {
                if (absoluteIndex++ < offset) {
                    continue;
                }

                if (steps.size() >= limit) {
                    return steps;
                }

                Object annotation = findStepAnnotation(datasetItem, userId, stepIndex);

                steps.add(new ProjectAnnotationStepDto(
                        datasetItem.getId(),
                        datasetItem.getItemIndex(),
                        stepIndex,
                        totalSteps,
                        valueAsString(datasetItem.getContent().get(CONTENT_KEY_FILE_NAME)),
                        valueAsString(datasetItem.getContent().get(CONTENT_KEY_MIME_TYPE)),
                        definition.previewForStep(stepIndex),
                        hasAnnotationPayload(annotation),
                        annotation));
            }
        }

        return steps;
    }

    private Object normalizeAnnotationPayload(Object annotationPayload) {
        if (annotationPayload == null) {
            throw new InvalidProjectDatasetException("Annotation payload is required");
        }

        if (annotationPayload instanceof String payloadAsString && payloadAsString.trim().isEmpty()) {
            throw new InvalidProjectDatasetException("Annotation payload is required");
        }

        if (annotationPayload instanceof List<?> payloadAsList && payloadAsList.isEmpty()) {
            throw new InvalidProjectDatasetException("Annotation payload is required");
        }

        if (annotationPayload instanceof Map<?, ?> payloadAsMap && payloadAsMap.isEmpty()) {
            throw new InvalidProjectDatasetException("Annotation payload is required");
        }

        return annotationPayload;
    }

    private Map<String, Object> normalizeNerAnnotationPayload(Object annotationPayload) {
        if (!(annotationPayload instanceof Map<?, ?> rawPayload)) {
            throw new InvalidProjectDatasetException("NER annotation payload must be an object with entities");
        }

        Map<String, Object> payload = toMutableStringObjectMap(rawPayload);
        Object rawEntities = payload.get(NER_ANNOTATION_KEY_ENTITIES);

        if (!(rawEntities instanceof List<?> entitiesList) || entitiesList.isEmpty()) {
            throw new InvalidProjectDatasetException("NER annotation requires at least one entity");
        }

        List<Map<String, Object>> normalizedEntities = new ArrayList<>();
        Set<String> seenOffsets = new HashSet<>();

        for (Object rawEntity : entitiesList) {
            if (!(rawEntity instanceof Map<?, ?> rawEntityMap)) {
                throw new InvalidProjectDatasetException("Each NER entity must be an object");
            }

            Map<String, Object> entity = toMutableStringObjectMap(rawEntityMap);

            String label = trimStringValue(entity.get(NER_ANNOTATION_KEY_LABEL));
            String text = trimStringValue(entity.get(NER_ANNOTATION_KEY_TEXT));
            Integer startOffset = parseOffsetValue(entity.get(NER_ANNOTATION_KEY_START_OFFSET));
            Integer endOffset = parseOffsetValue(entity.get(NER_ANNOTATION_KEY_END_OFFSET));

            if (label == null || text == null || startOffset == null || endOffset == null) {
                throw new InvalidProjectDatasetException(
                        "Each NER entity requires label, text, startOffset and endOffset");
            }

            if (startOffset < 0 || endOffset <= startOffset) {
                throw new InvalidProjectDatasetException("NER entity offsets are invalid");
            }

            if (endOffset - startOffset != text.length()) {
                throw new InvalidProjectDatasetException("NER entity offsets must match selected text length");
            }

            String key = startOffset + ":" + endOffset + ":" + label.toLowerCase();
            if (!seenOffsets.add(key)) {
                continue;
            }

            Map<String, Object> normalizedEntity = new LinkedHashMap<>();
            normalizedEntity.put(NER_ANNOTATION_KEY_LABEL, label);
            normalizedEntity.put(NER_ANNOTATION_KEY_TEXT, text);
            normalizedEntity.put(NER_ANNOTATION_KEY_START_OFFSET, startOffset);
            normalizedEntity.put(NER_ANNOTATION_KEY_END_OFFSET, endOffset);
            normalizedEntities.add(normalizedEntity);
        }

        if (normalizedEntities.isEmpty()) {
            throw new InvalidProjectDatasetException("NER annotation requires at least one entity");
        }

        Map<String, Object> normalizedPayload = new LinkedHashMap<>();
        normalizedPayload.put(NER_ANNOTATION_KEY_ENTITIES, normalizedEntities);

        String notes = trimStringValue(payload.get(ANNOTATION_KEY_NOTES));
        if (notes != null) {
            normalizedPayload.put(ANNOTATION_KEY_NOTES, notes);
        }

        return normalizedPayload;
    }

    private String trimStringValue(Object value) {
        if (!(value instanceof String stringValue)) {
            return null;
        }

        return StringUtils.trimToNull(stringValue);
    }

    private Integer parseOffsetValue(Object value) {
        if (value instanceof Number numberValue) {
            double rawValue = numberValue.doubleValue();
            if (!Double.isFinite(rawValue) || rawValue % 1 != 0) {
                return null;
            }

            return (int) rawValue;
        }

        if (value instanceof String stringValue) {
            String normalizedValue = stringValue.trim();
            if (normalizedValue.isEmpty()) {
                return null;
            }

            try {
                return Integer.parseInt(normalizedValue);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        return null;
    }

    private int resolveStepIndex(Integer requestedStepIndex, int totalSteps) {
        int stepIndex = requestedStepIndex == null ? 0 : requestedStepIndex;

        if (stepIndex < 0 || stepIndex >= totalSteps) {
            throw new InvalidProjectDatasetException("Invalid annotation step index");
        }

        return stepIndex;
    }

    private void storeStepAnnotation(DatasetItem datasetItem, Long userId, int stepIndex, Object annotationPayload) {
        Map<String, Object> content = ensureMutableContent(datasetItem);
        Map<String, Object> annotationsByUser = getOrCreateNestedMap(content, CONTENT_KEY_ANNOTATIONS_BY_USER);
        Map<String, Object> userAnnotations = getOrCreateNestedMap(annotationsByUser, String.valueOf(userId));
        Map<String, Object> steps = getOrCreateNestedMap(userAnnotations, USER_ANNOTATION_STEPS_KEY);
        steps.put(String.valueOf(stepIndex), annotationPayload);
    }

    private Object findStepAnnotation(DatasetItem datasetItem, Long userId, int stepIndex) {
        Map<String, Object> annotationsByUser = readAnnotationsByUser(datasetItem.getContent());
        Map<String, Object> userAnnotations = readNestedMap(annotationsByUser.get(String.valueOf(userId)));
        Map<String, Object> steps = readNestedMap(userAnnotations.get(USER_ANNOTATION_STEPS_KEY));
        return steps.get(String.valueOf(stepIndex));
    }

    private DatasetStepDefinition resolveStepDefinition(DatasetItem datasetItem) {
        if (isCsvDatasetItem(datasetItem)) {
            List<String> rowPreviews = parseCsvRowPreviews(datasetItem);
            return new DatasetStepDefinition(rowPreviews.size(), rowPreviews);
        }

        String fileName = valueAsString(datasetItem.getContent().get(CONTENT_KEY_FILE_NAME));
        if (fileName.isBlank()) {
            fileName = "Dataset item #" + (datasetItem.getItemIndex() + 1);
        }

        return new DatasetStepDefinition(1, List.of(fileName));
    }

    private boolean isCsvDatasetItem(DatasetItem datasetItem) {
        String mimeType = valueAsString(datasetItem.getContent().get(CONTENT_KEY_MIME_TYPE)).toLowerCase();
        String fileName = valueAsString(datasetItem.getContent().get(CONTENT_KEY_FILE_NAME)).toLowerCase();

        return mimeType.contains("csv") || fileName.endsWith(".csv");
    }

    private List<String> parseCsvRowPreviews(DatasetItem datasetItem) {
        String base64Content = valueAsString(datasetItem.getContent().get(CONTENT_KEY_BASE64));
        if (base64Content.isBlank()) {
            return List.of();
        }

        String csvContent;
        try {
            csvContent = new String(decodeStoredBase64(base64Content), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new InvalidProjectDatasetException("CSV dataset item has invalid Base64 content");
        }

        List<String> lines = csvContent.lines().toList();
        if (lines.isEmpty()) {
            return List.of();
        }
        String header = removeUtf8Bom(lines.get(0)).trim();

        if (lines.size() == 1) {
            if (header.isBlank()) {
                return List.of();
            }

            return List.of(truncatePreview(header));
        }

        List<String> previews = new ArrayList<>();

        for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
            String row = lines.get(lineIndex);
            if (row == null || row.isBlank()) {
                continue;
            }

            String normalizedRow = row.trim();
            if (header.isBlank()) {
                previews.add(truncatePreview(normalizedRow));
            } else {
                previews.add(buildCsvStepPreview(header, normalizedRow));
            }
        }

        return previews;
    }

    private String buildCsvStepPreview(String header, String row) {
        return truncatePreview(header) + "\n" + truncatePreview(row);
    }

    private String removeUtf8Bom(String value) {
        if (value.startsWith("\uFEFF")) {
            return value.substring(1);
        }

        return value;
    }

    private String truncatePreview(String value) {
        if (value.length() <= PREVIEW_MAX_LENGTH) {
            return value;
        }

        return value.substring(0, PREVIEW_MAX_LENGTH - 3) + "...";
    }

    private byte[] decodeStoredBase64(String rawBase64) {
        String normalizedBase64 = normalizeStoredBase64(rawBase64);
        return Base64.getMimeDecoder().decode(normalizedBase64);
    }

    private String normalizeStoredBase64(String rawBase64) {
        String trimmedBase64 = rawBase64 == null ? "" : rawBase64.trim();
        if (trimmedBase64.isEmpty()) {
            return trimmedBase64;
        }

        if (trimmedBase64.regionMatches(true, 0, "data:", 0, 5)) {
            int markerIndex = trimmedBase64.toLowerCase().indexOf("base64,");
            if (markerIndex >= 0) {
                return trimmedBase64.substring(markerIndex + "base64,".length()).trim();
            }
        }

        return trimmedBase64;
    }

    private Map<String, Object> ensureMutableContent(DatasetItem datasetItem) {
        Map<String, Object> content = datasetItem.getContent();
        if (content == null) {
            Map<String, Object> created = new LinkedHashMap<>();
            datasetItem.setContent(created);
            return created;
        }

        if (content instanceof LinkedHashMap<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> existing = (Map<String, Object>) content;
            return existing;
        }

        Map<String, Object> copied = new LinkedHashMap<>(content);
        datasetItem.setContent(copied);
        return copied;
    }

    private Map<String, Object> getOrCreateNestedMap(Map<String, Object> parent, String key) {
        Object rawValue = parent.get(key);
        if (rawValue instanceof Map<?, ?> rawMap) {
            Map<String, Object> normalized = toMutableStringObjectMap(rawMap);
            parent.put(key, normalized);
            return normalized;
        }

        Map<String, Object> created = new LinkedHashMap<>();
        parent.put(key, created);
        return created;
    }

    private Map<String, Object> readAnnotationsByUser(Map<String, Object> content) {
        if (content == null) {
            return Map.of();
        }

        Object rawAnnotations = content.get(CONTENT_KEY_ANNOTATIONS_BY_USER);
        return readNestedMap(rawAnnotations);
    }

    private Map<String, Object> readNestedMap(Object rawValue) {
        if (!(rawValue instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }

        return toMutableStringObjectMap(rawMap);
    }

    private Map<String, Object> toMutableStringObjectMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    private boolean hasAnnotationPayload(Object annotationPayload) {
        if (annotationPayload == null) {
            return false;
        }

        if (annotationPayload instanceof String stringValue) {
            return !stringValue.isBlank();
        }

        if (annotationPayload instanceof Map<?, ?> mapValue) {
            return !mapValue.isEmpty();
        }

        if (annotationPayload instanceof List<?> listValue) {
            return !listValue.isEmpty();
        }

        return true;
    }

    private void notifyProjectOwnersOnAnnotationCompletion(Project project, List<ProjectParticipant> participants,
            User actor) {
        for (ProjectParticipant projectParticipant : participants) {
            if (projectParticipant.getRole() != ProjectParticipantRole.CREATOR) {
                continue;
            }

            User owner = projectParticipant.getUser();
            if (owner.getId().equals(actor.getId())) {
                continue;
            }

            notificationService.createProjectAnnotationCompletedNotification(owner, actor, project);
        }
    }

    private ProjectProgressSnapshot buildProjectProgressSnapshot(Long projectId) {
        List<ProjectParticipant> participants = projectParticipantRepository
                .findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(projectId);
        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);
        return buildProjectProgressSnapshot(participants, datasetItems);
    }

    private ProjectProgressSnapshot buildProjectProgressSnapshot(
            List<ProjectParticipant> participants,
            List<DatasetItem> datasetItems) {
        Map<Long, Long> completedStepsByUser = new LinkedHashMap<>();
        for (ProjectParticipant participant : participants) {
            completedStepsByUser.put(participant.getUser().getId(), 0L);
        }

        long totalSteps = 0L;

        for (DatasetItem datasetItem : datasetItems) {
            DatasetStepDefinition stepDefinition = resolveStepDefinition(datasetItem);
            int itemTotalSteps = stepDefinition.totalSteps();
            totalSteps += itemTotalSteps;

            if (itemTotalSteps <= 0) {
                continue;
            }

            Map<String, Object> annotationsByUser = readAnnotationsByUser(datasetItem.getContent());
            for (Map.Entry<String, Object> userEntry : annotationsByUser.entrySet()) {
                Long userId = parseLong(userEntry.getKey());
                if (userId == null || !completedStepsByUser.containsKey(userId)) {
                    continue;
                }

                long itemCompletedSteps = countCompletedSteps(userEntry.getValue(), itemTotalSteps);
                completedStepsByUser.put(userId, completedStepsByUser.get(userId) + itemCompletedSteps);
            }
        }

        Map<Long, Integer> completionPercentageByUser = new LinkedHashMap<>();
        for (Map.Entry<Long, Long> entry : completedStepsByUser.entrySet()) {
            completionPercentageByUser.put(entry.getKey(), toPercentage(entry.getValue(), totalSteps));
        }

        long totalCompletedSteps = completedStepsByUser.values().stream()
                .mapToLong(Long::longValue)
                .sum();
        long totalPossibleSteps = totalSteps * completedStepsByUser.size();

        return new ProjectProgressSnapshot(
                totalSteps,
                toPercentage(totalCompletedSteps, totalPossibleSteps),
                completedStepsByUser,
                completionPercentageByUser);
    }

    private long countCompletedSteps(Object userAnnotationData, int maxSteps) {
        Map<String, Object> userData = readNestedMap(userAnnotationData);
        Map<String, Object> steps = readNestedMap(userData.get(USER_ANNOTATION_STEPS_KEY));

        Set<Integer> validStepIndexes = new HashSet<>();
        for (Map.Entry<String, Object> stepEntry : steps.entrySet()) {
            Integer stepIndex = parseInteger(stepEntry.getKey());
            if (stepIndex == null || stepIndex < 0 || stepIndex >= maxSteps) {
                continue;
            }

            if (!hasAnnotationPayload(stepEntry.getValue())) {
                continue;
            }

            validStepIndexes.add(stepIndex);
        }

        return validStepIndexes.size();
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int toPercentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }

        long boundedNumerator = Math.max(0L, Math.min(numerator, denominator));
        return (int) Math.round((boundedNumerator * 100.0) / denominator);
    }

    private User findUserByEmail(String authenticatedEmail) {
        String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);

        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));
    }

    private DatasetItemDto toDatasetItemDto(DatasetItem item) {
        String fileName = valueAsString(item.getContent().get(CONTENT_KEY_FILE_NAME));
        String mimeType = valueAsString(item.getContent().get(CONTENT_KEY_MIME_TYPE));
        long sizeBytes = valueAsLong(item.getContent().get(CONTENT_KEY_SIZE_BYTES));

        return new DatasetItemDto(
                item.getId(),
                item.getItemIndex(),
                fileName,
                mimeType,
                sizeBytes,
                item.getCreatedAt());
    }

    private String valueAsString(Object value) {
        return value == null ? "" : String.valueOf(value);
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
        User participantUser = participant.getUser();

        return new ProjectDetailParticipantDto(
                participantUser.getId(),
                participantUser.getFirstName(),
                participantUser.getLastName(),
                participantUser.getEmail(),
                participant.getRole(),
                completionPercentage);
    }

    private int calculateCompletionPercentage(Long projectId) {
        return buildProjectProgressSnapshot(projectId).projectCompletionPercentage();
    }

    private int calculateParticipantCompletionPercentage(Long projectId, Long userId) {
        return buildProjectProgressSnapshot(projectId).completionPercentageForUser(userId);
    }

    private record DatasetStepDefinition(int totalSteps, List<String> previews) {
        String previewForStep(int stepIndex) {
            if (stepIndex >= 0 && stepIndex < previews.size()) {
                return previews.get(stepIndex);
            }

            return "Step " + (stepIndex + 1);
        }
    }

    private record ProjectProgressSnapshot(
            long totalSteps,
            int projectCompletionPercentage,
            Map<Long, Long> completedStepsByUser,
            Map<Long, Integer> completionPercentageByUser) {

        long completedStepsForUser(Long userId) {
            return completedStepsByUser.getOrDefault(userId, 0L);
        }

        int completionPercentageForUser(Long userId) {
            return completionPercentageByUser.getOrDefault(userId, 0);
        }
    }
}
