package es.udc.fic.corpuslab.modules.project.services;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.exceptions.EmailNotFoundException;
import es.udc.fic.corpuslab.modules.auth.repositories.UserRepository;
import es.udc.fic.corpuslab.modules.auth.utils.EmailNormalizer;
import es.udc.fic.corpuslab.modules.project.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.DatasetItemDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupResponseDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSummaryDto;
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

    private final UserRepository userRepository;
    private final ResearchGroupRepository researchGroupRepository;
    private final ResearchGroupMemberRepository researchGroupMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final DatasetItemRepository datasetItemRepository;

    public ProjectServiceImpl(
            UserRepository userRepository,
            ResearchGroupRepository researchGroupRepository,
            ResearchGroupMemberRepository researchGroupMemberRepository,
            ProjectRepository projectRepository,
            ProjectParticipantRepository projectParticipantRepository,
            DatasetItemRepository datasetItemRepository) {
        this.userRepository = userRepository;
        this.researchGroupRepository = researchGroupRepository;
        this.researchGroupMemberRepository = researchGroupMemberRepository;
        this.projectRepository = projectRepository;
        this.projectParticipantRepository = projectParticipantRepository;
        this.datasetItemRepository = datasetItemRepository;
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

        projectRepository.findByIdAndResearchGroupId(projectId, researchGroupId)
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

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

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

        String guidelineText = trimToNull(request.guidelineText());
        String guidelinePdfBase64 = trimToNull(request.guidelinePdfBase64());
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
                String normalizedName = trimToNull(label.name());
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

        String normalizedGuidelineText = trimToNull(guidelineText);
        String normalizedGuidelinePdf = trimToNull(guidelinePdfBase64);

        if (normalizedGuidelineText == null && normalizedGuidelinePdf == null) {
            throw new InvalidProjectSetupException("Provide either a guideline text or a guideline PDF");
        }

        if (normalizedGuidelineText != null && normalizedGuidelinePdf != null) {
            throw new InvalidProjectSetupException("Guideline text and guideline PDF are mutually exclusive");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeHexColor(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase();
    }

    private User findUserByEmail(String authenticatedEmail) {
        String normalizedEmail = EmailNormalizer.canonicalizeGoogleEmail(authenticatedEmail);

        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new EmailNotFoundException(normalizedEmail));
    }

    private DatasetItemDto toDatasetItemDto(DatasetItem item) {
        String fileName = valueAsString(item.getContent().get("fileName"));
        String mimeType = valueAsString(item.getContent().get("mimeType"));
        long sizeBytes = valueAsLong(item.getContent().get("sizeBytes"));

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
}
