package es.udc.fic.corpuslab.modules.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardAnnotationTrendDatumDto;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardAnnotationTrendSeriesDto;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardAnnotationTrendsDto;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardMetricsDto;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardProjectDto;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardProjectsDto;
import es.udc.fic.corpuslab.modules.project.metrics.ProjectMetricsCacheService;
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.AnnotationRepository.AnnotationTrendRow;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectDatasetUtils;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final int DASHBOARD_PROJECT_LIMIT = 2;
    private static final int TREND_DAYS = 7;
    private static final String[] TREND_SERIES_COLORS = {
            "#312e81",
            "#047857",
            "#d97706",
            "#be123c",
            "#0f766e",
            "#7c3aed",
            "#0369a1"
    };

    private final AuthApiService authApiService;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final DatasetItemRepository datasetItemRepository;
    private final AnnotationRepository annotationRepository;
    private final ProjectMetricsCacheService projectMetricsCacheService;

    public DashboardServiceImpl(
            AuthApiService authApiService,
            ProjectParticipantRepository projectParticipantRepository,
            DatasetItemRepository datasetItemRepository,
            AnnotationRepository annotationRepository,
            ProjectMetricsCacheService projectMetricsCacheService) {
        this.authApiService = authApiService;
        this.projectParticipantRepository = projectParticipantRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.annotationRepository = annotationRepository;
        this.projectMetricsCacheService = projectMetricsCacheService;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardMetricsDto getMetrics(String authenticatedEmail) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);
        Long userId = userInfo.userId();

        List<ProjectParticipant> activeParticipants = findActiveParticipants(userId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant todayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant tomorrowStart = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant yesterdayStart = today.minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        long pendingAnnotations = 0L;
        long pendingProjects = 0L;
        for (ProjectParticipant participant : activeParticipants) {
            long projectPendingAnnotations = calculatePendingAnnotations(participant.getProject().getId(), userId);
            pendingAnnotations += projectPendingAnnotations;
            if (projectPendingAnnotations > 0) {
                pendingProjects++;
            }
        }

        long annotationsToday = annotationRepository.countByUserIdAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
                userId,
                todayStart,
                tomorrowStart);
        long annotationsYesterday = annotationRepository.countByUserIdAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
                userId,
                yesterdayStart,
                todayStart);
        long activeResearchGroups = activeParticipants.stream()
                .map(participant -> participant.getProject().getResearchGroup().getId())
                .distinct()
                .count();
        long openAlerts = annotationRepository.countActiveWarningsByUserId(userId);
        long alertsSinceYesterday = annotationRepository.countActiveWarningsByUserIdAndWarningMarkedAtGreaterThanEqual(
                userId,
                yesterdayStart);

        return new DashboardMetricsDto(
                activeParticipants.size(),
                activeResearchGroups,
                annotationsToday,
                annotationsYesterday,
                pendingAnnotations,
                pendingProjects,
                openAlerts,
                alertsSinceYesterday);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardAnnotationTrendsDto getAnnotationTrends(String authenticatedEmail) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate firstDay = today.minusDays(TREND_DAYS - 1L);
        Instant startInclusive = firstDay.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endExclusive = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<AnnotationTrendRow> trendRows = annotationRepository.findDailyAnnotationTrendsByUserId(
                userInfo.userId(),
                startInclusive,
                endExclusive);

        Map<String, DashboardAnnotationTrendSeriesDto> seriesByDataKey = new LinkedHashMap<>();
        Map<LocalDate, Map<String, Long>> valuesByDate = initializeTrendValues(firstDay);

        for (AnnotationTrendRow row : trendRows) {
            if (row.getIsoDate() == null || row.getResearchGroupId() == null) {
                continue;
            }

            String dataKey = toResearchGroupDataKey(row.getResearchGroupId());
            seriesByDataKey.computeIfAbsent(dataKey, key -> new DashboardAnnotationTrendSeriesDto(
                    key,
                    row.getResearchGroupName(),
                    TREND_SERIES_COLORS[seriesByDataKey.size() % TREND_SERIES_COLORS.length]));

            Map<String, Long> dayValues = valuesByDate.get(row.getIsoDate());
            if (dayValues != null) {
                dayValues.put(dataKey, row.getAnnotationCount() == null ? 0L : row.getAnnotationCount());
            }
        }

        if (seriesByDataKey.isEmpty()) {
            return new DashboardAnnotationTrendsDto(List.of(), List.of());
        }

        List<DashboardAnnotationTrendDatumDto> data = valuesByDate.entrySet().stream()
                .map(entry -> new DashboardAnnotationTrendDatumDto(entry.getKey(), entry.getValue()))
                .toList();

        return new DashboardAnnotationTrendsDto(
                List.copyOf(seriesByDataKey.values()),
                data);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardProjectsDto getProjects(String authenticatedEmail) {
        UserInfo userInfo = authApiService.findUserByEmail(authenticatedEmail);
        Long userId = userInfo.userId();

        List<DashboardProjectDto> projects = findActiveParticipants(userId).stream()
                .map(participant -> toDashboardProjectDto(participant, userId))
                .toList();

        List<DashboardProjectDto> recentProjects = projects.stream()
                .sorted(Comparator.comparing(DashboardProjectDto::updatedAt).reversed())
                .limit(DASHBOARD_PROJECT_LIMIT)
                .toList();
        List<DashboardProjectDto> advancedProjects = projects.stream()
                .sorted(Comparator.comparingInt(DashboardProjectDto::completionPercentage)
                        .reversed()
                        .thenComparing(Comparator.comparing(DashboardProjectDto::updatedAt).reversed()))
                .limit(DASHBOARD_PROJECT_LIMIT)
                .toList();

        return new DashboardProjectsDto(recentProjects, advancedProjects);
    }

    private List<ProjectParticipant> findActiveParticipants(Long userId) {
        return projectParticipantRepository.findActiveByUserIdWithProjectAndResearchGroup(userId);
    }

    private Map<LocalDate, Map<String, Long>> initializeTrendValues(LocalDate firstDay) {
        Map<LocalDate, Map<String, Long>> valuesByDate = new LinkedHashMap<>();
        for (int dayOffset = 0; dayOffset < TREND_DAYS; dayOffset++) {
            valuesByDate.put(firstDay.plusDays(dayOffset), new LinkedHashMap<>());
        }
        return valuesByDate;
    }

    private DashboardProjectDto toDashboardProjectDto(ProjectParticipant participant, Long userId) {
        Project project = participant.getProject();
        Long projectId = project.getId();

        return new DashboardProjectDto(
                projectId,
                project.getName(),
                project.getDescription(),
                projectMetricsCacheService.getProjectCompletionPercentage(projectId),
                calculatePendingAnnotations(projectId, userId),
                participant.getRole(),
                project.getResearchGroup().getName(),
                resolveProjectActivityAt(project));
    }

    private Instant resolveProjectActivityAt(Project project) {
        if (project.getLastActivityAt() != null) {
            return project.getLastActivityAt();
        }

        if (project.getUpdatedAt() != null) {
            return project.getUpdatedAt();
        }

        return project.getCreatedAt();
    }

    private long calculatePendingAnnotations(Long projectId, Long userId) {
        long totalSteps = countProjectSteps(projectId);
        long completedSteps = Math.min(
                totalSteps,
                annotationRepository.countByDatasetItemProjectIdAndUserId(projectId, userId));

        return Math.max(0L, totalSteps - completedSteps);
    }

    private long countProjectSteps(Long projectId) {
        return datasetItemRepository.findByProjectIdOrderByItemIndexAsc(projectId).stream()
                .mapToLong(this::countDatasetItemSteps)
                .sum();
    }

    private long countDatasetItemSteps(DatasetItem datasetItem) {
        return ProjectDatasetUtils.resolveStepDefinition(datasetItem).totalSteps();
    }

    private String toResearchGroupDataKey(Long researchGroupId) {
        return "researchGroup" + researchGroupId;
    }
}
