package es.udc.fic.corpuslab.modules.project.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectMetricsDto;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.IaaResultStatus;
import es.udc.fic.corpuslab.modules.project.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantIaaGroup;
import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.exceptions.ProjectNotFoundException;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectParticipantTestBuilder;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectTestBuilder;
import es.udc.fic.corpuslab.modules.project.iaa.calculators.IaaMetricCalculator;
import es.udc.fic.corpuslab.modules.project.iaa.context.AnnotationCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.dtos.IaaResult;
import es.udc.fic.corpuslab.modules.project.iaa.factory.MetricsFactory;
import es.udc.fic.corpuslab.modules.project.iaa.transformers.xrr.XrrAnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectParticipantRepository;

@ExtendWith(MockitoExtension.class)
class ProjectMetricsServiceImplTest {

    @Mock private AuthApiService authApiService;
    @Mock private ProjectParticipantRepository projectParticipantRepository;
    @Mock private DatasetItemRepository datasetItemRepository;
    @Mock private AnnotationRepository annotationRepository;
    @Mock private MetricsFactory metricsFactory;
    @Mock private IaaMetricCalculator calculator;

    private ProjectMetricsService projectMetricsService;

    @BeforeEach
    void setUp() {
        ProjectMetricsCalculator metricsCalculator = new ProjectMetricsCalculator(
                projectParticipantRepository,
                datasetItemRepository,
                annotationRepository,
                metricsFactory);
        ProjectProgressCalculator progressCalculator = new ProjectProgressCalculator(
                projectParticipantRepository,
                datasetItemRepository,
                annotationRepository);
        ProjectMetricsCacheService projectMetricsCacheService = new ProjectMetricsCacheService(
                metricsCalculator,
                progressCalculator);
        projectMetricsService = new ProjectMetricsServiceImpl(
                authApiService,
                projectParticipantRepository,
                projectMetricsCacheService);
    }

    @Test
    void getProjectMetricsShouldBuildContextAndReturnCompatibleMetrics() {
        User user = UserTestBuilder.validUser().withEmail("annotator@example.com").build();
        setId(user, 1L);
        User groupOneUser = UserTestBuilder.validUser().withEmail("group-one@example.com").build();
        setId(groupOneUser, 2L);
        User groupTwoUser = UserTestBuilder.validUser().withEmail("group-two@example.com").build();
        setId(groupTwoUser, 3L);

        Project project = ProjectTestBuilder.validProject()
                .withProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE)
                .build();
        setProjectId(project, 100L);
        project.setAnnotationTargetColumn("text");

        ProjectParticipant requesterParticipant = ProjectParticipantTestBuilder.validParticipant()
                .withUser(user)
                .withProject(project)
                .withRole(ProjectParticipantRole.CREATOR)
                .build();
        ProjectParticipant groupOneParticipant = ProjectParticipantTestBuilder.validParticipant()
                .withUser(groupOneUser)
                .withProject(project)
                .build();
        groupOneParticipant.setIaaGroup(ProjectParticipantIaaGroup.GROUP_A);
        ProjectParticipant groupTwoParticipant = ProjectParticipantTestBuilder.validParticipant()
                .withUser(groupTwoUser)
                .withProject(project)
                .build();
        groupTwoParticipant.setIaaGroup(ProjectParticipantIaaGroup.GROUP_B);

        DatasetItem datasetItem = new DatasetItem();
        datasetItem.setProject(project);
        datasetItem.setItemIndex(0);
        datasetItem.setContent(Map.of("text", "sample"));

        IaaResult expectedMetric = IaaResult.calculable(
                MetricType.COHENS_KAPPA,
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                0.75,
                1,
                1,
                0,
                Map.of());

        when(authApiService.findUserByEmail("annotator@example.com"))
                .thenReturn(new UserInfo(1L, "annotator@example.com", "Ann", "Otator"));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(requesterParticipant));
        when(projectParticipantRepository.findByProjectIdWithUserAndProject(100L))
                .thenReturn(List.of(requesterParticipant, groupOneParticipant, groupTwoParticipant));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of(datasetItem));
        when(annotationRepository.findByProjectIdWithDatasetItemAndUser(100L)).thenReturn(List.of());
        when(metricsFactory.supportedMetrics(ProjectType.TEXT_CLASSIFICATION_SIMPLE))
                .thenReturn(Set.of(MetricType.COHENS_KAPPA));
        when(metricsFactory.getCalculator(ProjectType.TEXT_CLASSIFICATION_SIMPLE, MetricType.COHENS_KAPPA))
                .thenReturn(calculator);
        when(calculator.calculate(any())).thenReturn(expectedMetric);

        ProjectMetricsDto result = projectMetricsService.getProjectMetrics("annotator@example.com", 100L);

        assertThat(result.projectId()).isEqualTo(100L);
        assertThat(result.metrics()).containsExactly(expectedMetric);

        ArgumentCaptor<AnnotationCalculationContext> contextCaptor =
                ArgumentCaptor.forClass(AnnotationCalculationContext.class);
        verify(calculator).calculate(contextCaptor.capture());
        assertThat(contextCaptor.getValue().projectId()).isEqualTo(100L);
        assertThat(contextCaptor.getValue().metadata()).containsEntry("annotationTargetColumn", "text");
        assertThat(contextCaptor.getValue().metadata())
                .containsEntry(XrrAnnotationDataTransformer.METADATA_KEY_GROUP_X_ANNOTATOR_IDS, List.of(2L))
                .containsEntry(XrrAnnotationDataTransformer.METADATA_KEY_GROUP_Y_ANNOTATOR_IDS, List.of(3L));
    }

    @Test
    void getProjectMetricsShouldReturnNonCalculableMetricWhenCalculatorFails() {
        User user = UserTestBuilder.validUser().withEmail("annotator@example.com").build();
        setId(user, 1L);

        Project project = ProjectTestBuilder.validProject()
                .withProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE)
                .build();
        setProjectId(project, 100L);

        ProjectParticipant requesterParticipant = ProjectParticipantTestBuilder.validParticipant()
                .withUser(user)
                .withProject(project)
                .build();

        when(authApiService.findUserByEmail("annotator@example.com"))
                .thenReturn(new UserInfo(1L, "annotator@example.com", "Ann", "Otator"));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(requesterParticipant));
        when(projectParticipantRepository.findByProjectIdWithUserAndProject(100L))
                .thenReturn(List.of(requesterParticipant));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of());
        when(annotationRepository.findByProjectIdWithDatasetItemAndUser(100L)).thenReturn(List.of());
        when(metricsFactory.supportedMetrics(ProjectType.TEXT_CLASSIFICATION_SIMPLE))
                .thenReturn(Set.of(MetricType.COHENS_KAPPA));
        when(metricsFactory.getCalculator(ProjectType.TEXT_CLASSIFICATION_SIMPLE, MetricType.COHENS_KAPPA))
                .thenReturn(calculator);
        when(calculator.calculate(any())).thenThrow(new IllegalArgumentException("bad payload"));

        ProjectMetricsDto result = projectMetricsService.getProjectMetrics("annotator@example.com", 100L);

        assertThat(result.metrics()).hasSize(1);
        IaaResult metric = result.metrics().get(0);
        assertThat(metric.metricType()).isEqualTo(MetricType.COHENS_KAPPA);
        assertThat(metric.calculable()).isFalse();
        assertThat(metric.status()).isEqualTo(IaaResultStatus.UNDEFINED);
        assertThat(metric.value()).isNaN();
        assertThat(metric.message()).contains("bad payload");
    }

    @Test
    void getProjectMetricsShouldHideUnassignedProjectsAsNotFound() {
        when(authApiService.findUserByEmail("outsider@example.com"))
                .thenReturn(new UserInfo(9L, "outsider@example.com", "Out", "Sider"));
        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 9L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMetricsService.getProjectMetrics("outsider@example.com", 100L))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    private void setId(User user, Long id) {
        try {
            java.lang.reflect.Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
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
