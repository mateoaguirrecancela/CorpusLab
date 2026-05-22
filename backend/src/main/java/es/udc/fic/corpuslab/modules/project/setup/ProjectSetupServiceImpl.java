package es.udc.fic.corpuslab.modules.project.setup;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.project.guideline.ProjectGuidelineService;
import es.udc.fic.corpuslab.modules.project.metrics.ProjectMetricsCacheService;
import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupRequestDto;
import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupResponseDto;
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.entities.Guideline;
import es.udc.fic.corpuslab.modules.project.shared.entities.Label;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.ProjectNotFoundException;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectConstants;
import es.udc.fic.corpuslab.modules.researchgroup.api.ResearchGroupApiService;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupMemberInfo;

@Service
public class ProjectSetupServiceImpl implements ProjectSetupService {

    private final AuthApiService authApiService;
    private final ResearchGroupApiService researchGroupApiService;
    private final ProjectRepository projectRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final ProjectGuidelineService projectGuidelineService;
    private final ProjectMetricsCacheService projectMetricsCacheService;
    private final LabelValidator labelValidator;
    private final ProjectSetupValidator projectSetupValidator;
    private final NerDatasetCompatibilityValidator nerDatasetCompatibilityValidator;

    public ProjectSetupServiceImpl(
            AuthApiService authApiService,
            ResearchGroupApiService researchGroupApiService,
            ProjectRepository projectRepository,
            DatasetItemRepository datasetItemRepository,
            ProjectGuidelineService projectGuidelineService,
            ProjectMetricsCacheService projectMetricsCacheService,
            LabelValidator labelValidator,
            ProjectSetupValidator projectSetupValidator,
            NerDatasetCompatibilityValidator nerDatasetCompatibilityValidator) {
        this.authApiService = authApiService;
        this.researchGroupApiService = researchGroupApiService;
        this.projectRepository = projectRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.projectGuidelineService = projectGuidelineService;
        this.projectMetricsCacheService = projectMetricsCacheService;
        this.labelValidator = labelValidator;
        this.projectSetupValidator = projectSetupValidator;
        this.nerDatasetCompatibilityValidator = nerDatasetCompatibilityValidator;
    }

    @Override
    @Transactional
    public ProjectSetupResponseDto configureProjectSetup(String authenticatedEmail, Long researchGroupId,
            Long projectId, ProjectSetupRequestDto request, MultipartFile guidelinePdfFile) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        Project project = projectRepository.findByIdAndResearchGroupId(projectId, researchGroupId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        ResearchGroupMemberInfo requesterMembership = researchGroupApiService
                .findActiveMember(researchGroupId, userInfo.userId())
                .orElseThrow(() -> new AccessDeniedException(ProjectConstants.NOT_MEMBER_ERROR));

        if (!requesterMembership.isOwnerOrAdmin()) {
            throw new AccessDeniedException("Only owners or admins can configure project setup");
        }

        List<ProjectSetupLabelDto> normalizedLabels = labelValidator.normalizeLabels(request.labels());
        String normalizedAnnotationTargetColumn = StringUtils.trimToNull(request.annotationTargetColumn());
        String normalizedGuidelinePdfBase64 = projectGuidelineService.normalizeAndValidateGuidelinePdfFile(
                guidelinePdfFile);

        List<DatasetItem> datasetItems = datasetItemRepository.findByProjectIdOrderByItemIndexAsc(project.getId());
        projectSetupValidator.validate(
                request.projectType(),
                normalizedLabels,
                request.guidelineText(),
                normalizedGuidelinePdfBase64,
                normalizedAnnotationTargetColumn,
                datasetItems);

        if (request.projectType() == ProjectType.NER) {
            nerDatasetCompatibilityValidator.validate(datasetItems);
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
        guideline.setContent(guidelineText);
        guideline.setFileUrl(normalizedGuidelinePdfBase64);

        project.markUpdated();
        projectRepository.save(project);
        projectMetricsCacheService.evictProjectReadCaches(projectId);

        return new ProjectSetupResponseDto(
                project.getId(),
                project.getProjectType(),
                normalizedLabels,
                guidelineText,
                project.getAnnotationTargetColumn(),
                project.isSetupCompleted());
    }

}
