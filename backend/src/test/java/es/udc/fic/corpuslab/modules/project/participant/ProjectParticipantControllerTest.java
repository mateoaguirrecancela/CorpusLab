package es.udc.fic.corpuslab.modules.project.participant;

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

import es.udc.fic.corpuslab.modules.project.participant.dtos.ProjectAssignableMemberDto;
import es.udc.fic.corpuslab.modules.project.participant.dtos.ProjectAssignmentContextDto;
import es.udc.fic.corpuslab.modules.project.participant.dtos.ProjectParticipantAssignmentDto;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantIaaGroup;

@ExtendWith(MockitoExtension.class)
class ProjectParticipantControllerTest {

    @Mock
    private ProjectParticipantService projectParticipantService;

    @Mock
    private Authentication authentication;

    private ProjectParticipantController controller;

    @BeforeEach
    void setUp() {
        controller = new ProjectParticipantController(projectParticipantService);
        when(authentication.getName()).thenReturn("owner@example.com");
    }

    @Test
    void getProjectAssignmentContextShouldDelegateToService() {
        ProjectAssignmentContextDto expected = new ProjectAssignmentContextDto(
                10L,
                20L,
                List.of(new ProjectAssignableMemberDto(20L, "Ann", "Otator", "ann@example.com")),
                List.of(new ProjectParticipantAssignmentDto(20L, ProjectParticipantIaaGroup.GROUP_A)));
        when(projectParticipantService.getProjectAssignmentContext("owner@example.com", 10L))
                .thenReturn(expected);

        ProjectAssignmentContextDto result = controller.getProjectAssignmentContext(authentication, 10L);

        assertThat(result).isEqualTo(expected);
        verify(projectParticipantService).getProjectAssignmentContext("owner@example.com", 10L);
    }
}
