package es.udc.fic.corpuslab.modules.project.guideline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;

import es.udc.fic.corpuslab.modules.project.guideline.dtos.ProjectGuidelinePdfContentDto;

@ExtendWith(MockitoExtension.class)
class ProjectGuidelineControllerTest {

    @Mock
    private ProjectGuidelineService projectGuidelineService;

    @Mock
    private Authentication authentication;

    private ProjectGuidelineController controller;

    @BeforeEach
    void setUp() {
        controller = new ProjectGuidelineController(projectGuidelineService);
        when(authentication.getName()).thenReturn("owner@example.com");
    }

    @Test
    void getProjectGuidelinePdfShouldBuildInlinePdfResponse() {
        byte[] payload = "%PDF-1.7\nbody".getBytes(StandardCharsets.UTF_8);
        when(projectGuidelineService.getProjectGuidelinePdf("owner@example.com", 88L))
                .thenReturn(new ProjectGuidelinePdfContentDto("guide.pdf", "application/pdf", payload));

        var response = controller.getProjectGuidelinePdf(authentication, 88L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/pdf");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(payload.length);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("inline")
                .contains("guide.pdf");
        assertThat(response.getBody()).isEqualTo(payload);
        verify(projectGuidelineService).getProjectGuidelinePdf("owner@example.com", 88L);
    }
}
