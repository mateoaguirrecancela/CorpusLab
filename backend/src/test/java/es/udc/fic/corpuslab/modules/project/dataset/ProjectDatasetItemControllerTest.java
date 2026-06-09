package es.udc.fic.corpuslab.modules.project.dataset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class ProjectDatasetItemControllerTest {

    @Mock
    private ProjectDatasetItemService projectDatasetItemService;

    @Mock
    private ProjectDatasetUploadQueue projectDatasetUploadQueue;

    @Mock
    private ProjectDatasetUploadEvents projectDatasetUploadEvents;

    @Mock
    private Authentication authentication;

    private ProjectDatasetItemController controller;

    @BeforeEach
    void setUp() {
        controller = new ProjectDatasetItemController(
                projectDatasetItemService,
                projectDatasetUploadQueue,
                projectDatasetUploadEvents);
        when(authentication.getName()).thenReturn("owner@example.com");
    }

    @Test
    void getDatasetUploadEventsShouldDelegateToEventsService() {
        SseEmitter expected = new SseEmitter();
        when(projectDatasetUploadEvents.open("owner@example.com", 77L, "job-123"))
                .thenReturn(expected);

        SseEmitter result = controller.getDatasetUploadEvents(authentication, 77L, "job-123");

        assertThat(result).isSameAs(expected);
        verify(projectDatasetUploadEvents).open("owner@example.com", 77L, "job-123");
    }
}
