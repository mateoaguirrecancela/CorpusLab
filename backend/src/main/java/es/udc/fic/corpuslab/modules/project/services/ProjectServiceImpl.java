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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
import es.udc.fic.corpuslab.modules.notification.repositories.NotificationRepository;
import es.udc.fic.corpuslab.modules.notification.services.NotificationService;
import es.udc.fic.corpuslab.modules.project.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.DatasetItemDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationExportCsvDto;
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
import es.udc.fic.corpuslab.modules.project.dtos.UpdateProjectRequestDto;
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
    private static final String ANNOTATION_KEY_BINARY_VALUE = "isExplanationCorrect";
    private static final String ANNOTATION_KEY_LABEL = "label";
    private static final String ANNOTATION_KEY_LABELS = "labels";
    private static final String ANNOTATION_KEY_TEXT = "text";
    private static final String EXPORT_ANNOTATION_HEADER_SUFFIX = "_annotation";
    private static final String EXPORT_COMMENT_HEADER_SUFFIX = "_coment";
    private static final int DEFAULT_ANNOTATION_STEPS_LIMIT = 50;
    private static final int MAX_ANNOTATION_STEPS_LIMIT = 250;
    private static final int PREVIEW_MAX_LENGTH = 160;

    private final UserRepository userRepository;
    private final ResearchGroupRepository researchGroupRepository;
    private final ResearchGroupMemberRepository researchGroupMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public ProjectServiceImpl(
            UserRepository userRepository,
            ResearchGroupRepository researchGroupRepository,
            ResearchGroupMemberRepository researchGroupMemberRepository,
            ProjectRepository projectRepository,
            ProjectParticipantRepository projectParticipantRepository,
            DatasetItemRepository datasetItemRepository,
            NotificationRepository notificationRepository,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.researchGroupRepository = researchGroupRepository;
        this.researchGroupMemberRepository = researchGroupMemberRepository;
        this.projectRepository = projectRepository;
        this.projectParticipantRepository = projectParticipantRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.notificationRepository = notificationRepository;
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
                project.getAnnotationTargetColumn(),
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
    public ProjectDetailDto updateProject(
            String authenticatedEmail,
            Long researchGroupId,
            Long projectId,
            UpdateProjectRequestDto request) {
        User requester = findUserByEmail(authenticatedEmail);

        Project project = projectRepository.findByIdAndResearchGroupId(projectId, researchGroupId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        ProjectParticipant requesterParticipant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, requester.getId())
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

        replaceProjectParticipants(project, requester, researchGroupId, request.participantUserIds());

        return getAssignedProjectDetail(authenticatedEmail, projectId);
    }

    @Override
    @Transactional
    public void deleteProject(
            String authenticatedEmail,
            Long researchGroupId,
            Long projectId) {
        User requester = findUserByEmail(authenticatedEmail);

        Project project = projectRepository.findByIdAndResearchGroupId(projectId, researchGroupId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        ProjectParticipant requesterParticipant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, requester.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (requesterParticipant.getRole() != ProjectParticipantRole.CREATOR) {
            throw new AccessDeniedException("Only project creators can delete projects");
        }

        notificationRepository.deleteByProjectId(projectId);
        datasetItemRepository.deleteByProjectId(projectId);
        projectParticipantRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
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

        boolean hasCsvFile = files.stream().anyMatch(file -> {
            String mimeType = file.getContentType();
            String name = file.getOriginalFilename();
            return (mimeType != null && mimeType.toLowerCase().contains("csv"))
                    || (name != null && name.toLowerCase().endsWith(".csv"));
        });

        if (hasCsvFile && files.size() > 1) {
            throw new InvalidProjectDatasetException("When uploading a CSV dataset, only a single file is allowed");
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

        replaceProjectParticipants(project, requester, researchGroupId, participantUserIds);
    }

    private void replaceProjectParticipants(
            Project project,
            User requester,
            Long researchGroupId,
            List<Long> participantUserIds) {
        Long projectId = project.getId();

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
                .filter(p -> p.getRole() == ProjectParticipantRole.PARTICIPANT && removedParticipantIds.contains(p.getUser().getId()))
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

        List<User> usersToAssign = userRepository.findAllById(newParticipantIds);
        if (usersToAssign.size() != newParticipantIds.size()) {
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

        Map<Long, User> usersById = usersToAssign.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        for (Long newParticipantId : newParticipantIds) {
            User recipient = usersById.get(newParticipantId);
            if (recipient != null) {
                notificationService.createProjectParticipantAssignedNotification(recipient, requester, project);
            }
        }
    }

    private void removeStoredAnnotationsForUsers(Long projectId, Set<Long> removedUserIds) {
        if (removedUserIds == null || removedUserIds.isEmpty()) {
            return;
        }

        List<String> removedUserKeys = removedUserIds.stream()
                .map(String::valueOf)
                .toList();

        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);
        List<DatasetItem> changedItems = new ArrayList<>();

        for (DatasetItem datasetItem : datasetItems) {
            Map<String, Object> content = ensureMutableContent(datasetItem);
            Object rawAnnotationsByUser = content.get(CONTENT_KEY_ANNOTATIONS_BY_USER);
            if (!(rawAnnotationsByUser instanceof Map<?, ?> rawMap)) {
                continue;
            }

            Map<String, Object> annotationsByUser = toMutableStringObjectMap(rawMap);
            boolean removedAnyAnnotation = false;

            for (String removedUserKey : removedUserKeys) {
                if (annotationsByUser.remove(removedUserKey) != null) {
                    removedAnyAnnotation = true;
                }
            }

            if (!removedAnyAnnotation) {
                continue;
            }

            if (annotationsByUser.isEmpty()) {
                content.remove(CONTENT_KEY_ANNOTATIONS_BY_USER);
            } else {
                content.put(CONTENT_KEY_ANNOTATIONS_BY_USER, annotationsByUser);
            }

            changedItems.add(datasetItem);
        }

        if (!changedItems.isEmpty()) {
            datasetItemRepository.saveAll(changedItems);
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
                project.getAnnotationTargetColumn(),
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
    @Transactional(readOnly = true)
    public ProjectAnnotationExportCsvDto exportAnnotationResultsCsv(
            String authenticatedEmail,
            Long projectId) {
        User requester = findUserByEmail(authenticatedEmail);

        ProjectParticipant requesterParticipant = projectParticipantRepository
                .findByProjectIdAndUserId(projectId, requester.getId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (requesterParticipant.getRole() != ProjectParticipantRole.CREATOR) {
            throw new AccessDeniedException("Only project creators can export annotation results");
        }

        Project project = requesterParticipant.getProject();
        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId);

        List<AnnotatorExportColumn> annotatorColumns = buildAnnotatorExportColumns(projectId);

        LinkedHashSet<String> csvColumns = new LinkedHashSet<>();
        List<ExportStepRow> exportRows = new ArrayList<>();

        for (DatasetItem datasetItem : datasetItems) {
            DatasetStepDefinition stepDefinition = resolveStepDefinition(datasetItem);
            int totalSteps = stepDefinition.totalSteps();

            for (int stepIndex = 0; stepIndex < totalSteps; stepIndex++) {
                Map<String, String> rowValues = stepDefinition.rowValuesForStep(stepIndex);
                if (rowValues != null && !rowValues.isEmpty()) {
                    csvColumns.addAll(rowValues.keySet());
                }

                Map<Long, Object> annotationsByUser = new LinkedHashMap<>();
                for (AnnotatorExportColumn annotatorColumn : annotatorColumns) {
                    annotationsByUser.put(
                            annotatorColumn.userId(),
                            findStepAnnotation(datasetItem, annotatorColumn.userId(), stepIndex));
                }

                exportRows.add(new ExportStepRow(
                        datasetItem,
                        stepIndex,
                        stepDefinition.previewForStep(stepIndex),
                        rowValues == null ? Map.of() : rowValues,
                        annotationsByUser));
            }
        }

        boolean isCsvDataset = datasetItems.stream().anyMatch(this::isCsvDatasetItem);

        List<String> headers = new ArrayList<>();
        if (!isCsvDataset) {
            headers.add("dataset_item_index");
            headers.add("source_name");
        }
        headers.addAll(csvColumns);

        for (AnnotatorExportColumn annotatorColumn : annotatorColumns) {
            headers.add(annotatorColumn.annotationHeader());
            headers.add(annotatorColumn.commentHeader());
        }

        StringBuilder csvBuilder = new StringBuilder();
        appendCsvLine(csvBuilder, headers);

        for (ExportStepRow row : exportRows) {
            DatasetItem datasetItem = row.datasetItem();
            List<String> values = new ArrayList<>();

            if (!isCsvDataset) {
                values.add(String.valueOf(datasetItem.getItemIndex()));
                values.add(valueAsString(datasetItem.getContent().get(CONTENT_KEY_FILE_NAME)));
            }

            for (String csvColumn : csvColumns) {
                values.add(row.rowValues().getOrDefault(csvColumn, ""));
            }

            for (AnnotatorExportColumn annotatorColumn : annotatorColumns) {
                Object annotation = row.annotationsByUser().get(annotatorColumn.userId());
                values.add(extractAnnotationValue(annotation));
                values.add(extractCommentValue(annotation));
            }

            appendCsvLine(csvBuilder, values);
        }

        return new ProjectAnnotationExportCsvDto(
                buildAnnotationExportFileName(project),
                csvBuilder.toString().getBytes(StandardCharsets.UTF_8));
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

        if ((projectType == ProjectType.TEXT_CLASSIFICATION_SIMPLE || projectType == ProjectType.TEXT_CLASSIFICATION_MULTILABEL) && labels.size() < 2) {
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
        boolean hasCsvDataset = datasetItems.stream().anyMatch(this::isCsvDatasetItem);

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
            if (!isCsvDatasetItem(datasetItem)) {
                continue;
            }

            List<String> headerColumns = parseCsvHeaderColumns(datasetItem);
            boolean columnExists = headerColumns.stream()
                    .anyMatch(headerColumn -> headerColumn.equalsIgnoreCase(annotationTargetColumn));

            if (!columnExists) {
                throw new InvalidProjectSetupException(
                        "Selected annotation target column '" + annotationTargetColumn
                                + "' was not found in CSV file " + describeDatasetItem(datasetItem));
            }
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
                    "NER projects require at least one dataset file in text, JSON or CSV format");
        }

        List<String> unsupportedFiles = datasetItems.stream()
                .filter(datasetItem -> !isNerCompatibleDatasetItem(datasetItem))
                .map(this::describeDatasetItem)
                .limit(5)
                .toList();

        if (!unsupportedFiles.isEmpty()) {
            throw new InvalidProjectSetupException(
                    "NER projects only support text, JSON or CSV files. Unsupported files: "
                            + String.join(", ", unsupportedFiles));
        }
    }

    private boolean isNerCompatibleDatasetItem(DatasetItem datasetItem) {
        String mimeType = valueAsString(datasetItem.getContent().get(CONTENT_KEY_MIME_TYPE)).trim().toLowerCase();
        String extension = extractFileExtension(valueAsString(datasetItem.getContent().get(CONTENT_KEY_FILE_NAME)));

        if (mimeType.equals("text/plain")
                || mimeType.equals("application/json")
                || mimeType.equals("text/json")
                || mimeType.endsWith("+json")
                || mimeType.contains("csv")) {
            return true;
        }

        return extension.equals("txt") || extension.equals("json") || extension.equals("csv");
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
                        definition.rowValuesForStep(stepIndex),
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
            CsvDatasetContent parsedCsv = parseCsvDatasetContent(datasetItem);
            return new DatasetStepDefinition(parsedCsv.steps());
        }

        String fileName = valueAsString(datasetItem.getContent().get(CONTENT_KEY_FILE_NAME));
        if (fileName.isBlank()) {
            fileName = "Dataset item #" + (datasetItem.getItemIndex() + 1);
        }

        return new DatasetStepDefinition(List.of(new DatasetStepData(fileName, null)));
    }

    private boolean isCsvDatasetItem(DatasetItem datasetItem) {
        String mimeType = valueAsString(datasetItem.getContent().get(CONTENT_KEY_MIME_TYPE)).toLowerCase();
        String fileName = valueAsString(datasetItem.getContent().get(CONTENT_KEY_FILE_NAME)).toLowerCase();

        return mimeType.contains("csv") || fileName.endsWith(".csv");
    }

    private CsvDatasetContent parseCsvDatasetContent(DatasetItem datasetItem) {
        String base64Content = valueAsString(datasetItem.getContent().get(CONTENT_KEY_BASE64));
        if (base64Content.isBlank()) {
            return new CsvDatasetContent(List.of(), List.of());
        }

        String csvContent;
        try {
            csvContent = new String(decodeStoredBase64(base64Content), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new InvalidProjectDatasetException("CSV dataset item has invalid Base64 content");
        }

        String[] rawLines = csvContent.split("\\r?\\n", -1);
        List<String> lines = List.of(rawLines);
        if (lines.isEmpty()) {
            return new CsvDatasetContent(List.of(), List.of());
        }

        String header = removeUtf8Bom(lines.get(0)).trim();
        List<String> rawHeaderColumns = parseCsvColumns(header);
        List<DatasetStepData> steps = new ArrayList<>();

        for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
            String row = lines.get(lineIndex);
            if (row == null || row.isBlank()) {
                continue;
            }

            String normalizedRow = row.trim();
            List<String> rowColumns = parseCsvColumns(normalizedRow);
            List<String> normalizedHeaders = normalizeCsvHeaderColumns(rawHeaderColumns, rowColumns.size());
            Map<String, String> rowValues = toCsvRowValues(normalizedHeaders, rowColumns);

            if (header.isBlank()) {
                steps.add(new DatasetStepData(truncatePreview(normalizedRow), rowValues));
            } else {
                steps.add(new DatasetStepData(buildCsvStepPreview(header, normalizedRow), rowValues));
            }
        }

        List<String> headerColumns = normalizeCsvHeaderColumns(rawHeaderColumns, rawHeaderColumns.size());
        return new CsvDatasetContent(headerColumns, steps);
    }

    private List<String> parseCsvHeaderColumns(DatasetItem datasetItem) {
        CsvDatasetContent csvDatasetContent = parseCsvDatasetContent(datasetItem);
        return csvDatasetContent.headers();
    }

    private List<String> normalizeCsvHeaderColumns(List<String> rawHeaderColumns, int minColumns) {
        int requiredColumns = Math.max(minColumns, rawHeaderColumns.size());
        List<String> normalizedHeaders = new ArrayList<>(requiredColumns);
        Set<String> seen = new LinkedHashSet<>();

        for (int index = 0; index < requiredColumns; index++) {
            String candidate = index < rawHeaderColumns.size()
                    ? StringUtils.trimToNull(rawHeaderColumns.get(index))
                    : null;

            if (candidate == null) {
                candidate = "column_" + (index + 1);
            }

            String normalizedCandidate = candidate;
            int duplicateIndex = 2;
            while (!seen.add(normalizedCandidate.toLowerCase())) {
                normalizedCandidate = candidate + "_" + duplicateIndex++;
            }

            normalizedHeaders.add(normalizedCandidate);
        }

        return normalizedHeaders;
    }

    private Map<String, String> toCsvRowValues(List<String> headers, List<String> rowColumns) {
        if (headers.isEmpty()) {
            return Map.of();
        }

        Map<String, String> rowValues = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            String value = index < rowColumns.size() ? rowColumns.get(index) : "";
            rowValues.put(headers.get(index), value);
        }

        return rowValues;
    }

    private List<String> parseCsvColumns(String line) {
        if (line == null || line.isBlank()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean insideQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);

            if (currentChar == '"') {
                if (insideQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    currentValue.append('"');
                    index++;
                } else {
                    insideQuotes = !insideQuotes;
                }
                continue;
            }

            if (currentChar == ',' && !insideQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
                continue;
            }

            currentValue.append(currentChar);
        }

        values.add(currentValue.toString().trim());
        return values;
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

    private void appendCsvLine(StringBuilder csvBuilder, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                csvBuilder.append(',');
            }

            csvBuilder.append(escapeCsvValue(values.get(index)));
        }

        csvBuilder.append('\n');
    }

    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }

        boolean mustBeQuoted = value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r");

        if (!mustBeQuoted) {
            return value;
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String buildAnnotationExportFileName(Project project) {
        String projectName = StringUtils.trimToNull(project.getName());
        if (projectName == null) {
            return "project-" + project.getId() + "-annotations.csv";
        }

        String slugifiedName = projectName
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");

        if (slugifiedName.isBlank()) {
            return "project-" + project.getId() + "-annotations.csv";
        }

        return slugifiedName + "-annotations.csv";
    }

    private List<AnnotatorExportColumn> buildAnnotatorExportColumns(Long projectId) {
        Map<Long, String> annotatorEmailByUserId = new LinkedHashMap<>();

        for (ProjectParticipant projectParticipant : projectParticipantRepository
                .findByProjectIdOrderByRoleAscUserLastNameAscUserFirstNameAsc(projectId)) {
            User annotator = projectParticipant.getUser();
            if (annotator == null || annotator.getId() == null) {
                continue;
            }

            annotatorEmailByUserId.putIfAbsent(annotator.getId(), normalizeExportAnnotatorEmail(annotator));
        }

        return annotatorEmailByUserId.entrySet().stream()
                .map(entry -> new AnnotatorExportColumn(
                        entry.getKey(),
                        entry.getValue() + EXPORT_ANNOTATION_HEADER_SUFFIX,
                        entry.getValue() + EXPORT_COMMENT_HEADER_SUFFIX))
                .toList();
    }

    private String normalizeExportAnnotatorEmail(User annotator) {
        String normalizedEmail = StringUtils.trimToNull(annotator.getEmail());
        if (normalizedEmail == null) {
            return "user-" + annotator.getId();
        }

        return normalizedEmail.toLowerCase(Locale.ROOT);
    }

    private String extractAnnotationValue(Object annotationPayload) {
        if (!hasAnnotationPayload(annotationPayload)) {
            return "";
        }

        String normalizedBoolean = normalizeBooleanValue(annotationPayload);
        if (normalizedBoolean != null) {
            return normalizedBoolean;
        }

        if (annotationPayload instanceof String annotationAsString) {
            return annotationAsString.trim();
        }

        if (annotationPayload instanceof Number || annotationPayload instanceof Boolean) {
            return String.valueOf(annotationPayload);
        }

        if (annotationPayload instanceof List<?> annotationAsList) {
            return normalizeListLikeValue(annotationAsList);
        }

        if (!(annotationPayload instanceof Map<?, ?> annotationAsMap)) {
            return String.valueOf(annotationPayload);
        }

        Map<String, Object> annotationMap = toMutableStringObjectMap(annotationAsMap);

        String labelValue = StringUtils.trimToNull(valueAsString(annotationMap.get(ANNOTATION_KEY_LABEL)));
        if (labelValue != null) {
            return labelValue;
        }

        String labelsValue = normalizeListLikeValue(annotationMap.get(ANNOTATION_KEY_LABELS));
        if (!labelsValue.isBlank()) {
            return labelsValue;
        }

        String textValue = StringUtils.trimToNull(valueAsString(annotationMap.get(ANNOTATION_KEY_TEXT)));
        if (textValue != null) {
            return textValue;
        }

        String entitiesValue = normalizeNerEntitiesForExport(annotationMap.get(NER_ANNOTATION_KEY_ENTITIES));
        if (!entitiesValue.isBlank()) {
            return entitiesValue;
        }

        String binaryValue = normalizeBooleanValue(annotationMap.get(ANNOTATION_KEY_BINARY_VALUE));
        if (binaryValue != null) {
            return binaryValue;
        }

        return annotationMap.toString();
    }

    private String extractCommentValue(Object annotationPayload) {
        if (!(annotationPayload instanceof Map<?, ?> annotationAsMap)) {
            return "";
        }

        Map<String, Object> annotationMap = toMutableStringObjectMap(annotationAsMap);
        String notes = StringUtils.trimToNull(valueAsString(annotationMap.get(ANNOTATION_KEY_NOTES)));

        return notes == null ? "" : notes;
    }

    private String normalizeNerEntitiesForExport(Object rawEntities) {
        if (!(rawEntities instanceof List<?> entities)) {
            return "";
        }

        List<String> normalizedEntities = entities.stream()
                .map(rawEntity -> {
                    if (!(rawEntity instanceof Map<?, ?> rawEntityMap)) {
                        return null;
                    }

                    Map<String, Object> entityMap = toMutableStringObjectMap(rawEntityMap);
                    String label = StringUtils.trimToNull(valueAsString(entityMap.get(NER_ANNOTATION_KEY_LABEL)));
                    String text = StringUtils.trimToNull(valueAsString(entityMap.get(NER_ANNOTATION_KEY_TEXT)));

                    if (label != null && text != null) {
                        return label + ":" + text;
                    }

                    if (text != null) {
                        return text;
                    }

                    return label;
                })
                .filter(entityValue -> entityValue != null && !entityValue.isBlank())
                .distinct()
                .toList();

        return String.join("|", normalizedEntities);
    }

    private String normalizeListLikeValue(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof String valueAsString) {
            String normalized = valueAsString.trim();
            return normalized.isEmpty() ? "" : normalized;
        }

        if (!(value instanceof List<?> valueAsList)) {
            return "";
        }

        List<String> normalizedValues = valueAsList.stream()
                .filter(entry -> entry != null)
                .map(String::valueOf)
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .distinct()
                .toList();

        return String.join("|", normalizedValues);
    }

    private String normalizeBooleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return String.valueOf(booleanValue);
        }

        if (value instanceof Number numberValue) {
            long asLong = numberValue.longValue();
            if (asLong == 0L || asLong == 1L) {
                return String.valueOf(asLong == 1L);
            }

            return null;
        }

        if (!(value instanceof String stringValue)) {
            return null;
        }

        String normalized = stringValue.trim().toLowerCase();
        if (normalized.equals("true") || normalized.equals("false")) {
            return normalized;
        }

        return null;
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

    private record DatasetStepDefinition(List<DatasetStepData> steps) {
        int totalSteps() {
            return steps.size();
        }

        String previewForStep(int stepIndex) {
            if (stepIndex >= 0 && stepIndex < steps.size()) {
                return steps.get(stepIndex).preview();
            }

            return "Step " + (stepIndex + 1);
        }

        Map<String, String> rowValuesForStep(int stepIndex) {
            if (stepIndex >= 0 && stepIndex < steps.size()) {
                return steps.get(stepIndex).rowValues();
            }

            return null;
        }
    }

    private record DatasetStepData(
            String preview,
            Map<String, String> rowValues) {
    }

    private record CsvDatasetContent(
            List<String> headers,
            List<DatasetStepData> steps) {
    }

    private record ExportStepRow(
            DatasetItem datasetItem,
            int stepIndex,
            String preview,
            Map<String, String> rowValues,
            Map<Long, Object> annotationsByUser) {
    }

    private record AnnotatorExportColumn(
            Long userId,
            String annotationHeader,
            String commentHeader) {
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
}
