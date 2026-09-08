package es.udc.fic.corpuslab.modules.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardAnnotationTrendsDto;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardMetricsDto;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardProjectsDto;
import es.udc.fic.corpuslab.modules.project.metrics.ProjectMetricsCacheService;
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.shared.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.AnnotationRepository.AnnotationTrendRow;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private AuthApiService authApiService;

    @Mock
    private ProjectParticipantRepository projectParticipantRepository;

    @Mock
    private DatasetItemRepository datasetItemRepository;

    @Mock
    private AnnotationRepository annotationRepository;

    @Mock
    private ProjectMetricsCacheService projectMetricsCacheService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(
                authApiService,
                projectParticipantRepository,
                datasetItemRepository,
                annotationRepository,
                projectMetricsCacheService);
    }

    @Test
    void getMetricsShouldAggregatePendingAndActivityCounters() {
        Long userId = 7L;
        ProjectParticipant firstParticipant = participant(100L, "Project A", 11L, "Group One");
        ProjectParticipant secondParticipant = participant(101L, "Project B", 12L, "Group Two");

        when(authApiService.findUserByEmail("annotator@example.com"))
                .thenReturn(new UserInfo(userId, "annotator@example.com", "Ann", "Otator"));
        when(projectParticipantRepository.findActiveByUserIdWithProjectAndResearchGroup(userId))
                .thenReturn(List.of(firstParticipant, secondParticipant));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L))
                .thenReturn(List.of(textDatasetItem(0), textDatasetItem(1)));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(101L))
                .thenReturn(List.of(textDatasetItem(0)));
        when(annotationRepository.countByDatasetItemProjectIdAndUserId(100L, userId)).thenReturn(1L);
        when(annotationRepository.countByDatasetItemProjectIdAndUserId(101L, userId)).thenReturn(5L);
        when(annotationRepository.countByUserIdAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(eq(userId), any(), any()))
                .thenReturn(5L, 3L);
        when(annotationRepository.countActiveWarningsByUserId(userId)).thenReturn(2L);
        when(annotationRepository.countActiveWarningsByUserIdAndWarningMarkedAtGreaterThanEqual(eq(userId), any()))
                .thenReturn(1L);

        DashboardMetricsDto metrics = dashboardService.getMetrics("annotator@example.com");

        assertThat(metrics.activeProjects()).isEqualTo(2L);
        assertThat(metrics.activeResearchGroups()).isEqualTo(2L);
        assertThat(metrics.annotationsToday()).isEqualTo(5L);
        assertThat(metrics.annotationsYesterday()).isEqualTo(3L);
        assertThat(metrics.pendingAnnotations()).isEqualTo(1L);
        assertThat(metrics.pendingProjects()).isEqualTo(1L);
        assertThat(metrics.openAlerts()).isEqualTo(2L);
        assertThat(metrics.alertsSinceYesterday()).isEqualTo(1L);
    }

    @Test
    void getAnnotationTrendsShouldReturnEmptyPayloadWhenNoSeriesIsProduced() {
        Long userId = 7L;
        when(authApiService.findUserByEmail("annotator@example.com"))
                .thenReturn(new UserInfo(userId, "annotator@example.com", "Ann", "Otator"));
        when(annotationRepository.findDailyAnnotationTrendsByUserId(eq(userId), any(), any()))
                .thenReturn(List.of(
                        new StubTrendRow(null, "Invalid", LocalDate.now(ZoneOffset.UTC), 2L),
                        new StubTrendRow(10L, "Invalid", null, 4L)));

        DashboardAnnotationTrendsDto trends = dashboardService.getAnnotationTrends("annotator@example.com");

        assertThat(trends.series()).isEmpty();
        assertThat(trends.data()).isEmpty();
    }

    @Test
    void getAnnotationTrendsShouldBuildSeriesAndSevenDayTimeline() {
        Long userId = 7L;
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate firstDay = today.minusDays(6);
        LocalDate middleDay = today.minusDays(2);

        when(authApiService.findUserByEmail("annotator@example.com"))
                .thenReturn(new UserInfo(userId, "annotator@example.com", "Ann", "Otator"));
        when(annotationRepository.findDailyAnnotationTrendsByUserId(eq(userId), any(), any()))
                .thenReturn(List.of(
                        new StubTrendRow(11L, "Group One", firstDay, 3L),
                        new StubTrendRow(12L, "Group Two", middleDay, 5L),
                        new StubTrendRow(11L, "Group One", middleDay, 2L)));

        DashboardAnnotationTrendsDto trends = dashboardService.getAnnotationTrends("annotator@example.com");

        assertThat(trends.series()).hasSize(2);
        assertThat(trends.series().get(0).dataKey()).isEqualTo("researchGroup11");
        assertThat(trends.series().get(0).name()).isEqualTo("Group One");
        assertThat(trends.series().get(0).color()).isEqualTo("#312e81");
        assertThat(trends.series().get(1).dataKey()).isEqualTo("researchGroup12");
        assertThat(trends.series().get(1).color()).isEqualTo("#047857");

        assertThat(trends.data()).hasSize(7);
        assertThat(trends.data().get(0).isoDate()).isEqualTo(firstDay);
        assertThat(trends.data().get(6).isoDate()).isEqualTo(today);
        assertThat(trends.data().get(0).values()).containsEntry("researchGroup11", 3L);
        assertThat(trends.data().get(4).values())
                .containsEntry("researchGroup11", 2L)
                .containsEntry("researchGroup12", 5L);
    }

    @Test
    void getProjectsShouldSortRecentAndAdvancedViews() {
        Long userId = 7L;
        ProjectParticipant firstParticipant = participant(100L, "Project A", 11L, "Group One");
        ProjectParticipant secondParticipant = participant(101L, "Project B", 11L, "Group One");
        ProjectParticipant thirdParticipant = participant(102L, "Project C", 12L, "Group Two");
        setField(firstParticipant.getProject(), "lastActivityAt", Instant.parse("2026-05-01T10:15:30Z"));
        setField(secondParticipant.getProject(), "updatedAt", Instant.parse("2026-05-03T10:15:30Z"));
        setField(secondParticipant.getProject(), "createdAt", Instant.parse("2026-04-01T00:00:00Z"));
        setField(thirdParticipant.getProject(), "lastActivityAt", Instant.parse("2026-05-02T10:15:30Z"));

        when(authApiService.findUserByEmail("annotator@example.com"))
                .thenReturn(new UserInfo(userId, "annotator@example.com", "Ann", "Otator"));
        when(projectParticipantRepository.findActiveByUserIdWithProjectAndResearchGroup(userId))
                .thenReturn(List.of(firstParticipant, secondParticipant, thirdParticipant));

        when(projectMetricsCacheService.getProjectCompletionPercentage(100L)).thenReturn(60);
        when(projectMetricsCacheService.getProjectCompletionPercentage(101L)).thenReturn(90);
        when(projectMetricsCacheService.getProjectCompletionPercentage(102L)).thenReturn(30);

        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of(textDatasetItem(0)));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(101L))
                .thenReturn(List.of(textDatasetItem(0), textDatasetItem(1)));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(102L))
                .thenReturn(List.of(textDatasetItem(0), textDatasetItem(1), textDatasetItem(2)));
        when(annotationRepository.countByDatasetItemProjectIdAndUserId(100L, userId)).thenReturn(0L);
        when(annotationRepository.countByDatasetItemProjectIdAndUserId(101L, userId)).thenReturn(2L);
        when(annotationRepository.countByDatasetItemProjectIdAndUserId(102L, userId)).thenReturn(1L);

        DashboardProjectsDto projects = dashboardService.getProjects("annotator@example.com");

        assertThat(projects.recentProjects()).extracting(project -> project.id())
                .containsExactly(101L, 102L);
        assertThat(projects.advancedProjects()).extracting(project -> project.id())
                .containsExactly(101L, 100L);
        assertThat(projects.advancedProjects().get(0).pendingAnnotations()).isZero();
        assertThat(projects.advancedProjects().get(1).pendingAnnotations()).isEqualTo(1L);
    }

    private ProjectParticipant participant(Long projectId, String projectName, Long researchGroupId, String researchGroupName) {
        ResearchGroup researchGroup = new ResearchGroup();
        setField(researchGroup, "id", researchGroupId);
        researchGroup.setName(researchGroupName);

        Project project = new Project();
        setField(project, "id", projectId);
        project.setName(projectName);
        project.setDescription("Description for " + projectName);
        project.setResearchGroup(researchGroup);

        ProjectParticipant participant = new ProjectParticipant();
        participant.setProject(project);
        participant.setRole(ProjectParticipantRole.PARTICIPANT);
        return participant;
    }

    private DatasetItem textDatasetItem(int index) {
        DatasetItem item = new DatasetItem();
        item.setItemIndex(index);
        item.setContent(Map.of("text", "Sample " + index));
        return item;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static final class StubTrendRow implements AnnotationTrendRow {

        private final Long researchGroupId;
        private final String researchGroupName;
        private final LocalDate isoDate;
        private final Long annotationCount;

        private StubTrendRow(Long researchGroupId, String researchGroupName, LocalDate isoDate, Long annotationCount) {
            this.researchGroupId = researchGroupId;
            this.researchGroupName = researchGroupName;
            this.isoDate = isoDate;
            this.annotationCount = annotationCount;
        }

        @Override
        public Long getResearchGroupId() {
            return researchGroupId;
        }

        @Override
        public String getResearchGroupName() {
            return researchGroupName;
        }

        @Override
        public LocalDate getIsoDate() {
            return isoDate;
        }

        @Override
        public Long getAnnotationCount() {
            return annotationCount;
        }
    }
}
