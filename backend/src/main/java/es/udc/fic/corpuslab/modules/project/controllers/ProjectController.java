package es.udc.fic.corpuslab.modules.project.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.udc.fic.corpuslab.modules.project.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAssignedSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectDetailDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupResponseDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.UpdateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.services.ProjectService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/research-groups/{groupId}/projects/my")
    @ResponseStatus(HttpStatus.OK)
    public List<ProjectAssignedSummaryDto> findMyAssignedProjectsByGroup(
            Authentication authentication,
            @PathVariable Long groupId) {
        return projectService.findAssignedProjectsByResearchGroup(authentication.getName(), groupId);
    }

    @PostMapping("/research-groups/{groupId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectSummaryDto createProject(
            Authentication authentication,
            @PathVariable Long groupId,
            @Valid @RequestBody CreateProjectRequestDto request) {
        return projectService.createProject(authentication.getName(), groupId, request);
    }

    @PutMapping("/research-groups/{groupId}/projects/{projectId}")
    @ResponseStatus(HttpStatus.OK)
    public ProjectDetailDto updateProject(
            Authentication authentication,
            @PathVariable Long groupId,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequestDto request) {
        return projectService.updateProject(authentication.getName(), groupId, projectId, request);
    }

    @DeleteMapping("/research-groups/{groupId}/projects/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(
            Authentication authentication,
            @PathVariable Long groupId,
            @PathVariable Long projectId) {
        projectService.deleteProject(authentication.getName(), groupId, projectId);
    }

    @PutMapping("/research-groups/{groupId}/projects/{projectId}/setup")
    @ResponseStatus(HttpStatus.OK)
    public ProjectSetupResponseDto configureProjectSetup(
            Authentication authentication,
            @PathVariable Long groupId,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectSetupRequestDto request) {
        return projectService.configureProjectSetup(authentication.getName(), groupId, projectId, request);
    }

    @GetMapping("/projects/my")
    @ResponseStatus(HttpStatus.OK)
    public List<ProjectAssignedSummaryDto> findMyAssignedProjects(Authentication authentication) {
        return projectService.findMyAssignedProjects(authentication.getName());
    }

    @GetMapping("/projects/{projectId}")
    @ResponseStatus(HttpStatus.OK)
    public ProjectDetailDto getProjectDetail(
            Authentication authentication,
            @PathVariable Long projectId) {
        return projectService.getAssignedProjectDetail(authentication.getName(), projectId);
    }
}
