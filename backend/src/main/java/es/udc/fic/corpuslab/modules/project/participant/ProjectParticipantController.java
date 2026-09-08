package es.udc.fic.corpuslab.modules.project.participant;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.udc.fic.corpuslab.modules.project.participant.dtos.AssignProjectParticipantsRequestDto;
import es.udc.fic.corpuslab.modules.project.participant.dtos.ProjectAssignmentContextDto;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ProjectParticipantController {

    private final ProjectParticipantService projectParticipantService;

    public ProjectParticipantController(ProjectParticipantService projectParticipantService) {
        this.projectParticipantService = projectParticipantService;
    }

    @GetMapping("/research-groups/{groupId}/projects/assignment-context")
    @ResponseStatus(HttpStatus.OK)
    public ProjectAssignmentContextDto getProjectAssignmentContext(
            Authentication authentication,
            @PathVariable Long groupId) {
        return projectParticipantService.getProjectAssignmentContext(authentication.getName(), groupId);
    }

    @PostMapping("/research-groups/{groupId}/projects/{projectId}/participants")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignParticipants(
            Authentication authentication,
            @PathVariable("groupId") Long groupId,
            @PathVariable("projectId") Long projectId,
            @Valid @RequestBody AssignProjectParticipantsRequestDto request) {
        projectParticipantService.assignParticipants(
                authentication.getName(),
                groupId,
                projectId,
                request);
    }
}
