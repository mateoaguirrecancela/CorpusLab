package es.udc.fic.corpuslab.modules.project.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.udc.fic.corpuslab.modules.project.dtos.ProjectAssignedSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectDetailDto;
import es.udc.fic.corpuslab.modules.project.services.ProjectService;

@RestController
@RequestMapping("/api/projects")
public class ProjectQueryController {

    private final ProjectService projectService;

    public ProjectQueryController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/my")
    @ResponseStatus(HttpStatus.OK)
    public List<ProjectAssignedSummaryDto> findMyAssignedProjects(Authentication authentication) {
        return projectService.findMyAssignedProjects(authentication.getName());
    }

    @GetMapping("/{projectId}")
    @ResponseStatus(HttpStatus.OK)
    public ProjectDetailDto getProjectDetail(
            Authentication authentication,
            @PathVariable Long projectId) {
        return projectService.getAssignedProjectDetail(authentication.getName(), projectId);
    }
}
