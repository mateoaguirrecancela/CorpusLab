package es.udc.fic.corpuslab.modules.project.controllers;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.udc.fic.corpuslab.modules.project.dtos.ProjectAssignedSummaryDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationExportCsvDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationSourceContentDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationWorkspaceDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectDetailDto;
import es.udc.fic.corpuslab.modules.project.dtos.SaveProjectAnnotationStepRequestDto;
import es.udc.fic.corpuslab.modules.project.dtos.SaveProjectAnnotationStepResponseDto;
import es.udc.fic.corpuslab.modules.project.services.ProjectService;
import jakarta.validation.Valid;

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

    @GetMapping("/{projectId}/annotations/steps")
    @ResponseStatus(HttpStatus.OK)
    public ProjectAnnotationWorkspaceDto getAnnotationWorkspace(
            Authentication authentication,
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return projectService.getAnnotationWorkspace(authentication.getName(), projectId, offset, limit);
    }

    @GetMapping("/{projectId}/annotations/participants/{participantUserId}/steps")
    @ResponseStatus(HttpStatus.OK)
    public ProjectAnnotationWorkspaceDto getParticipantAnnotationWorkspaceForCreator(
            Authentication authentication,
            @PathVariable Long projectId,
            @PathVariable Long participantUserId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return projectService.getParticipantAnnotationWorkspaceForCreator(
                authentication.getName(),
                projectId,
                participantUserId,
                offset,
                limit);
    }

    @GetMapping("/{projectId}/dataset-items/{datasetItemId}/content")
    public ResponseEntity<byte[]> getAnnotationSourceContent(
            Authentication authentication,
            @PathVariable Long projectId,
            @PathVariable Long datasetItemId) {
        ProjectAnnotationSourceContentDto sourceContent = projectService
                .getAnnotationSourceContent(authentication.getName(), projectId, datasetItemId);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(sourceContent.mimeType());
        } catch (InvalidMediaTypeException ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        String fileName = sourceContent.fileName() == null || sourceContent.fileName().isBlank()
                ? "dataset-item-" + datasetItemId
                : sourceContent.fileName().replace("\"", "");

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(sourceContent.bytes());
    }

    @GetMapping("/{projectId}/annotations/export")
    public ResponseEntity<byte[]> exportAnnotationResultsCsv(
            Authentication authentication,
            @PathVariable Long projectId) {
        ProjectAnnotationExportCsvDto exportCsv = projectService
                .exportAnnotationResultsCsv(authentication.getName(), projectId);

        String fileName = exportCsv.fileName() == null || exportCsv.fileName().isBlank()
                ? "project-" + projectId + "-annotations.csv"
                : exportCsv.fileName().replace("\"", "");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(exportCsv.bytes());
    }

    @PutMapping("/{projectId}/annotations/steps")
    @ResponseStatus(HttpStatus.OK)
    public SaveProjectAnnotationStepResponseDto saveAnnotationStep(
            Authentication authentication,
            @PathVariable Long projectId,
            @Valid @RequestBody SaveProjectAnnotationStepRequestDto request) {
        return projectService.saveAnnotationStep(authentication.getName(), projectId, request);
    }
}
