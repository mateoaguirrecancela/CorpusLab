package es.udc.fic.corpuslab.modules.project.setup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.project.guideline.ProjectGuidelineService;
import es.udc.fic.corpuslab.modules.project.metrics.ProjectMetricsCacheService;
import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupRequestDto;
import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupResponseDto;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectTestBuilder;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.researchgroup.api.ResearchGroupApiService;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupMemberInfo;

@ExtendWith(MockitoExtension.class)
class ProjectSetupServiceImplTest {

    @Mock
    private AuthApiService authApiService;

    @Mock
    private ResearchGroupApiService researchGroupApiService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private DatasetItemRepository datasetItemRepository;

    @Mock
    private ProjectGuidelineService projectGuidelineService;

    @Mock
    private ProjectMetricsCacheService projectMetricsCacheService;

    @Test
    void configureProjectSetupShouldNormalizeLabelsAndPersistGuidelineText() {
        Project project = ProjectTestBuilder.validProject().build();
        setProjectId(project, 100L);
        ProjectSetupServiceImpl service = new ProjectSetupServiceImpl(
                authApiService,
                researchGroupApiService,
                projectRepository,
                datasetItemRepository,
                projectGuidelineService,
                projectMetricsCacheService,
                new LabelValidator(),
                new ProjectSetupValidator(new LabelValidator(), new AnnotationTargetColumnValidator()),
                new NerDatasetCompatibilityValidator());
        ProjectSetupRequestDto request = new ProjectSetupRequestDto(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(
                        new ProjectSetupLabelDto(" Positive ", "#AA00BB"),
                        new ProjectSetupLabelDto("Negative", null)),
                "  Read carefully  ",
                null);

        when(authApiService.findUserByEmail("owner@example.com"))
                .thenReturn(new UserInfo(1L, "owner@example.com", "Owner", "User"));
        when(projectRepository.findByIdAndResearchGroupId(100L, 10L)).thenReturn(Optional.of(project));
        when(researchGroupApiService.findActiveMember(10L, 1L))
                .thenReturn(Optional.of(new ResearchGroupMemberInfo(1L, "OWNER")));
        when(projectGuidelineService.normalizeAndValidateGuidelinePdfFile(null)).thenReturn(null);
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of());

        ProjectSetupResponseDto response = service.configureProjectSetup(
                "owner@example.com",
                10L,
                100L,
                request,
                null);

        assertThat(response.setupCompleted()).isTrue();
        assertThat(response.guidelineText()).isEqualTo("Read carefully");
        assertThat(response.labels()).containsExactly(
                new ProjectSetupLabelDto("Positive", "#AA00BB"),
                new ProjectSetupLabelDto("Negative", null));
        assertThat(project.getGuideline().getContent()).isEqualTo("Read carefully");
        assertThat(project.getLabels()).hasSize(2);
        verify(projectRepository).save(project);
        verify(projectMetricsCacheService).evictProjectReadCaches(100L);
    }

    private void setProjectId(Project project, Long id) {
        try {
            java.lang.reflect.Field field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
