package es.udc.fic.corpuslab.modules.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardAnnotationTrendDatumDto;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardAnnotationTrendSeriesDto;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardAnnotationTrendsDto;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardMetricsDto;
import es.udc.fic.corpuslab.modules.dashboard.dtos.DashboardProjectsDto;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private Authentication authentication;

    private DashboardController controller;

    @BeforeEach
    void setUp() {
        controller = new DashboardController(dashboardService);
        when(authentication.getName()).thenReturn("annotator@example.com");
    }

    @Test
    void metricsShouldDelegateToService() {
        DashboardMetricsDto expected = new DashboardMetricsDto(2, 1, 5, 4, 12, 2, 1, 0);
        when(dashboardService.getMetrics("annotator@example.com")).thenReturn(expected);

        DashboardMetricsDto result = controller.metrics(authentication);

        assertThat(result).isEqualTo(expected);
        verify(dashboardService).getMetrics("annotator@example.com");
    }

    @Test
    void annotationTrendsShouldDelegateToService() {
        DashboardAnnotationTrendsDto expected = new DashboardAnnotationTrendsDto(
                List.of(new DashboardAnnotationTrendSeriesDto("researchGroup10", "Group", "#312e81")),
                List.of(new DashboardAnnotationTrendDatumDto(LocalDate.of(2026, 5, 24), Map.of("researchGroup10", 3L))));
        when(dashboardService.getAnnotationTrends("annotator@example.com")).thenReturn(expected);

        DashboardAnnotationTrendsDto result = controller.annotationTrends(authentication);

        assertThat(result).isEqualTo(expected);
        verify(dashboardService).getAnnotationTrends("annotator@example.com");
    }

    @Test
    void projectsShouldDelegateToService() {
        DashboardProjectsDto expected = new DashboardProjectsDto(List.of(), List.of());
        when(dashboardService.getProjects("annotator@example.com")).thenReturn(expected);

        DashboardProjectsDto result = controller.projects(authentication);

        assertThat(result).isEqualTo(expected);
        verify(dashboardService).getProjects("annotator@example.com");
    }
}
