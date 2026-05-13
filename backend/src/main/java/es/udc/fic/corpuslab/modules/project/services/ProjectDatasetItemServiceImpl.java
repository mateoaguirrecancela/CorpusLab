package es.udc.fic.corpuslab.modules.project.services;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

import es.udc.fic.corpuslab.common.utils.FileSecurityUtil;
import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.project.dtos.DatasetItemDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationSourceContentDto;
import es.udc.fic.corpuslab.modules.project.dtos.UploadProjectDatasetResponseDto;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.exceptions.InvalidProjectDatasetException;
import es.udc.fic.corpuslab.modules.project.exceptions.ProjectNotFoundException;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.project.utils.ProjectConstants;
import es.udc.fic.corpuslab.modules.project.utils.ProjectDatasetUtils;
import es.udc.fic.corpuslab.modules.researchgroup.api.ResearchGroupApiService;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupMemberInfo;

@Service
public class ProjectDatasetItemServiceImpl implements ProjectDatasetItemService {

    private final ProjectRepository projectRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final AuthApiService authApiService;
    private final ResearchGroupApiService researchGroupApiService;
    private final ProjectMetricsCacheService projectMetricsCacheService;
    private final long maxStoredFileSizeBytes;

    @Autowired
    public ProjectDatasetItemServiceImpl(
            ProjectRepository projectRepository,
            DatasetItemRepository datasetItemRepository,
            ProjectParticipantRepository projectParticipantRepository,
            AuthApiService authApiService,
            ResearchGroupApiService researchGroupApiService,
            ProjectMetricsCacheService projectMetricsCacheService,
            @org.springframework.beans.factory.annotation.Value("${app.dataset.max-stored-file-size-bytes:10485760}") long maxStoredFileSizeBytes) {
        this.projectRepository = projectRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.projectParticipantRepository = projectParticipantRepository;
        this.authApiService = authApiService;
        this.researchGroupApiService = researchGroupApiService;
        this.projectMetricsCacheService = projectMetricsCacheService;
        this.maxStoredFileSizeBytes = maxStoredFileSizeBytes;
    }

    @Override
    @Transactional
    public UploadProjectDatasetResponseDto uploadDataset(String authenticatedEmail, Long researchGroupId,
            Long projectId, List<MultipartFile> files) {
        Project project = validateAndFindProject(authenticatedEmail, researchGroupId, projectId, files);

        int nextIndex = (int) datasetItemRepository.countByProjectId(projectId);
        List<DatasetItem> createdItems = new ArrayList<>();

        for (MultipartFile file : files) {
            FileSecurityUtil.validateFile(file);

            try {
                byte[] fileBytes = file.getBytes();
                String originalFilename = file.getOriginalFilename();
                String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

                if (ProjectDatasetUtils.isCsvFile(originalFilename, contentType)) {
                    fileBytes = FileSecurityUtil.sanitizeCsv(fileBytes);
                }

                if (fileBytes.length > maxStoredFileSizeBytes) {
                    throw new InvalidProjectDatasetException(
                            "File exceeds the maximum size allowed for dataset storage");
                }

                Map<String, Object> content = new LinkedHashMap<>();
                content.put(ProjectConstants.CONTENT_KEY_FILE_NAME, originalFilename);
                content.put(ProjectConstants.CONTENT_KEY_MIME_TYPE, contentType);
                content.put(ProjectConstants.CONTENT_KEY_SIZE_BYTES, fileBytes.length);
                content.put(ProjectConstants.CONTENT_KEY_BASE64, Base64.getEncoder().encodeToString(fileBytes));
                content.put("uploadedAt", Instant.now().toString());

                DatasetItem item = new DatasetItem();
                item.setProject(project);
                item.setItemIndex(nextIndex++);
                item.setContent(content);
                content.put(ProjectConstants.CONTENT_KEY_STEP_COUNT,
                        ProjectDatasetUtils.resolveStepDefinition(item).totalSteps());
                createdItems.add(item);
            } catch (IOException ex) {
                throw new InvalidProjectDatasetException("Could not read one of the uploaded files");
            }
        }

        List<DatasetItem> savedItems = datasetItemRepository.saveAll(createdItems);
        List<DatasetItemDto> itemDtos = savedItems.stream().map(this::toDatasetItemDto).toList();
        projectMetricsCacheService.evictProjectReadCaches(projectId);

        return new UploadProjectDatasetResponseDto(projectId, itemDtos.size(), itemDtos);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateDatasetUploadRequest(String authenticatedEmail, Long researchGroupId, Long projectId,
            List<MultipartFile> files) {
        validateAndFindProject(authenticatedEmail, researchGroupId, projectId, files);
        for (MultipartFile file : files) {
            FileSecurityUtil.validateFile(file);
        }
    }

    private Project validateAndFindProject(String authenticatedEmail, Long researchGroupId, Long projectId,
            List<MultipartFile> files) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        Project project = projectRepository.findByIdAndResearchGroupId(projectId, researchGroupId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (project.isSetupCompleted()) {
            throw new InvalidProjectDatasetException("Dataset cannot be changed after project setup is completed");
        }

        ResearchGroupMemberInfo requesterMembership = researchGroupApiService
                .findActiveMember(researchGroupId, userInfo.userId())
                .orElseThrow(() -> new AccessDeniedException(ProjectConstants.NOT_MEMBER_ERROR));

        if (!requesterMembership.isOwnerOrAdmin()) {
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

        return project;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectAnnotationSourceContentDto getAnnotationSourceContent(String authenticatedEmail, Long projectId,
            Long datasetItemId) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        projectParticipantRepository.findByProjectIdAndUserId(projectId, userInfo.userId())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        DatasetItem datasetItem = datasetItemRepository.findByIdAndProjectId(datasetItemId, projectId)
                .orElseThrow(() -> new InvalidProjectDatasetException("Dataset item does not belong to this project"));

        String base64Content = ProjectDatasetUtils
                .valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_BASE64));
        if (base64Content.isBlank()) {
            throw new InvalidProjectDatasetException("Dataset item has no binary content");
        }

        byte[] bytes;
        try {
            bytes = ProjectDatasetUtils.decodeStoredBase64(base64Content);
        } catch (IllegalArgumentException ex) {
            throw new InvalidProjectDatasetException("Dataset item has invalid Base64 content");
        }

        String mimeType = ProjectDatasetUtils
                .valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_MIME_TYPE));
        if (mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        return new ProjectAnnotationSourceContentDto(
                ProjectDatasetUtils
                        .valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_FILE_NAME)),
                mimeType,
                bytes);
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
}
