package es.udc.fic.corpuslab.modules.project.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import es.udc.fic.corpuslab.modules.project.metrics.dtos.ProjectMetricsDto;

@ExtendWith(MockitoExtension.class)
class ProjectMetricsControllerTest {

    @Mock
    private ProjectMetricsService projectMetricsService;

    @Mock
    private Authentication authentication;

    private ProjectMetricsController controller;

    @BeforeEach
    void setUp() {
        controller = new ProjectMetricsController(projectMetricsService);
        when(authentication.getName()).thenReturn("annotator@example.com");
    }

    @Test
    void getProjectMetricsShouldDelegateToService() {
        ProjectMetricsDto expected = new ProjectMetricsDto(100L, List.of());
        when(projectMetricsService.getProjectMetrics("annotator@example.com", 100L)).thenReturn(expected);

        ProjectMetricsDto result = controller.getProjectMetrics(authentication, 100L);

        assertThat(result).isEqualTo(expected);
        verify(projectMetricsService).getProjectMetrics("annotator@example.com", 100L);
    }
}
