package es.udc.fic.corpuslab.modules.project.core;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private Authentication authentication;

    private ProjectController controller;

    @BeforeEach
    void setUp() {
        controller = new ProjectController(projectService);
        when(authentication.getName()).thenReturn("owner@example.com");
    }

    @Test
    void cleanupIncompleteProjectShouldDelegateToService() {
        controller.cleanupIncompleteProject(authentication, 12L, 45L);

        verify(projectService).cleanupIncompleteProject("owner@example.com", 12L, 45L);
    }
}
