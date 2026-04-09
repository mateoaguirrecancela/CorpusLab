package es.udc.fic.corpuslab.modules.project.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.udc.fic.corpuslab.modules.project.dtos.AssignProjectParticipantsRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.CreateProjectRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAssignedSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupResponseDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.UploadProjectDatasetResponseDto;
import es.udc.fic.corpuslab.modules.project.services.ProjectService;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/research-groups/{groupId}/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping(path = "/my")
    @ResponseStatus(HttpStatus.OK)
    public List<ProjectAssignedSummaryDto> findMyAssignedProjectsByGroup(
            Authentication authentication,
            @PathVariable Long groupId) {
        return projectService.findAssignedProjectsByResearchGroup(authentication.getName(), groupId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectSummaryDto createProject(
            Authentication authentication,
            @PathVariable Long groupId,
            @Valid @RequestBody CreateProjectRequestDto request) {
        return projectService.createProject(authentication.getName(), groupId, request);
    }

    @PostMapping(path = "/{projectId}/dataset", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadProjectDatasetResponseDto uploadDataset(
            Authentication authentication,
            @PathVariable Long groupId,
            @PathVariable Long projectId,
            @RequestParam("files") List<MultipartFile> files) {
        return projectService.uploadDataset(authentication.getName(), groupId, projectId, files);
    }

    @PutMapping(path = "/{projectId}/setup")
    @ResponseStatus(HttpStatus.OK)
    public ProjectSetupResponseDto configureProjectSetup(
            Authentication authentication,
            @PathVariable Long groupId,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectSetupRequestDto request) {
        return projectService.configureProjectSetup(authentication.getName(), groupId, projectId, request);
    }

    @PostMapping(path = "/{projectId}/participants")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignParticipants(
            Authentication authentication,
            @PathVariable("groupId") Long groupId,
            @PathVariable("projectId") Long projectId,
            @RequestBody AssignProjectParticipantsRequestDto request) {
        projectService.assignParticipants(
                authentication.getName(),
                groupId,
                projectId,
                request.participantUserIds());
    }
}
