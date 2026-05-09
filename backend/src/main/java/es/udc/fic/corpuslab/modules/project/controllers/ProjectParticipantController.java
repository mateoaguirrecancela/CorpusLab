package es.udc.fic.corpuslab.modules.project.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.udc.fic.corpuslab.modules.project.dtos.AssignProjectParticipantsRequestDto;
import es.udc.fic.corpuslab.modules.project.services.ProjectParticipantService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ProjectParticipantController {

    private final ProjectParticipantService projectParticipantService;

    public ProjectParticipantController(ProjectParticipantService projectParticipantService) {
        this.projectParticipantService = projectParticipantService;
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
