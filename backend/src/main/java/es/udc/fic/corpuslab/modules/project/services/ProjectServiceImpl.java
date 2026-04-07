package es.udc.fic.corpuslab.modules.project.services;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.UploadProjectDatasetResponseDto;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.exceptions.InvalidProjectDatasetException;
import es.udc.fic.corpuslab.modules.project.exceptions.ProjectNotFoundException;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupMember;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;
import es.udc.fic.corpuslab.modules.researchgroup.exceptions.ResearchGroupNotFoundException;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupMemberRepository;
import es.udc.fic.corpuslab.modules.researchgroup.repositories.ResearchGroupRepository;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final UserRepository userRepository;
    private final ResearchGroupRepository researchGroupRepository;
    private final ResearchGroupMemberRepository researchGroupMemberRepository;
    private final ProjectRepository projectRepository;
    private final DatasetItemRepository datasetItemRepository;

    public ProjectServiceImpl(
            UserRepository userRepository,
            ResearchGroupRepository researchGroupRepository,
            ResearchGroupMemberRepository researchGroupMemberRepository,
            ProjectRepository projectRepository,
            DatasetItemRepository datasetItemRepository) {
        this.userRepository = userRepository;
        this.researchGroupRepository = researchGroupRepository;
        this.researchGroupMemberRepository = researchGroupMemberRepository;
        this.projectRepository = projectRepository;
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
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this research group"));

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
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this research group"));

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
