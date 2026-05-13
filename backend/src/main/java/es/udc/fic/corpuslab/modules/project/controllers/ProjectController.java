package es.udc.fic.corpuslab.modules.project.controllers;

import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.udc.fic.corpuslab.modules.project.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAssignedSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectDetailDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectMetricsDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupResponseDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.UpdateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.services.ProjectMetricsService;
import es.udc.fic.corpuslab.modules.project.services.ProjectService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMetricsService projectMetricsService;

    public ProjectController(ProjectService projectService, ProjectMetricsService projectMetricsService) {
        this.projectService = projectService;
        this.projectMetricsService = projectMetricsService;
    }

    @GetMapping("/research-groups/{groupId}/projects/my")
    @ResponseStatus(HttpStatus.OK)
    public Slice<ProjectAssignedSummaryDto> findMyAssignedProjectsByGroup(
            Authentication authentication,
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "false") boolean showArchived) {
        return projectService.findAssignedProjectsByResearchGroup(
                authentication.getName(),
                groupId,
                page,
                size,
                showArchived);
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

    @DeleteMapping("/research-groups/{groupId}/projects/{projectId}/wizard-cleanup")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cleanupIncompleteProject(
            Authentication authentication,
            @PathVariable Long groupId,
            @PathVariable Long projectId) {
        projectService.cleanupIncompleteProject(authentication.getName(), groupId, projectId);
    }

    @PutMapping("/projects/{projectId}/archive")
    @ResponseStatus(HttpStatus.OK)
    public ProjectDetailDto archiveProject(
            Authentication authentication,
            @PathVariable Long projectId) {
        return projectService.archiveProject(authentication.getName(), projectId);
    }

    @PutMapping("/projects/{projectId}/unarchive")
    @ResponseStatus(HttpStatus.OK)
    public ProjectDetailDto unarchiveProject(
            Authentication authentication,
            @PathVariable Long projectId) {
        return projectService.unarchiveProject(authentication.getName(), projectId);
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
    public Slice<ProjectAssignedSummaryDto> findMyAssignedProjects(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "false") boolean showArchived) {
        return projectService.findMyAssignedProjects(authentication.getName(), page, size, showArchived);
    }

    @GetMapping("/projects/{projectId}")
    @ResponseStatus(HttpStatus.OK)
    public ProjectDetailDto getProjectDetail(
            Authentication authentication,
            @PathVariable Long projectId) {
        return projectService.getAssignedProjectDetail(authentication.getName(), projectId);
    }

    @GetMapping("/projects/{projectId}/metrics")
    @ResponseStatus(HttpStatus.OK)
    public ProjectMetricsDto getProjectMetrics(
            Authentication authentication,
            @PathVariable Long projectId) {
        return projectMetricsService.getProjectMetrics(authentication.getName(), projectId);
    }
}
