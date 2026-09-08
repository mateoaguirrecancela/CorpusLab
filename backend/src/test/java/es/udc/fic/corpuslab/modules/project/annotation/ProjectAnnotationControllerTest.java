package es.udc.fic.corpuslab.modules.project.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import es.udc.fic.corpuslab.modules.project.annotation.dtos.ProjectAnnotationWarningResponseDto;

@ExtendWith(MockitoExtension.class)
class ProjectAnnotationControllerTest {

    @Mock
    private ProjectAnnotationService projectAnnotationService;

    @Mock
    private Authentication authentication;

    private ProjectAnnotationController controller;

    @BeforeEach
    void setUp() {
        controller = new ProjectAnnotationController(projectAnnotationService);
        when(authentication.getName()).thenReturn("creator@example.com");
    }

    @Test
    void toggleAnnotationWarningShouldDelegateToService() {
        var request = new ProjectAnnotationController.ToggleAnnotationWarningRequest(51L, 2);
        ProjectAnnotationWarningResponseDto expected = new ProjectAnnotationWarningResponseDto(100L, 51L, 2, true);
        when(projectAnnotationService.toggleAnnotationWarning("creator@example.com", 100L, 200L, 51L, 2))
                .thenReturn(expected);

        ProjectAnnotationWarningResponseDto result = controller.toggleAnnotationWarning(authentication, 100L, 200L,
                request);

        assertThat(result).isEqualTo(expected);
        verify(projectAnnotationService).toggleAnnotationWarning("creator@example.com", 100L, 200L, 51L, 2);
    }

    @Test
    void resolveOwnAnnotationWarningShouldDelegateToService() {
        var request = new ProjectAnnotationController.ResolveAnnotationWarningRequest(51L, 2);
        ProjectAnnotationWarningResponseDto expected = new ProjectAnnotationWarningResponseDto(100L, 51L, 2, false);
        when(projectAnnotationService.resolveOwnAnnotationWarning("creator@example.com", 100L, 51L, 2))
                .thenReturn(expected);

        ProjectAnnotationWarningResponseDto result = controller.resolveOwnAnnotationWarning(authentication, 100L,
                request);

        assertThat(result).isEqualTo(expected);
        verify(projectAnnotationService).resolveOwnAnnotationWarning("creator@example.com", 100L, 51L, 2);
    }
}
