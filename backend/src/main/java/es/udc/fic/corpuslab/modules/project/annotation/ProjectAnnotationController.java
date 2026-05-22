package es.udc.fic.corpuslab.modules.project.annotation;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.udc.fic.corpuslab.modules.project.annotation.dtos.ProjectAnnotationWorkspaceDto;
import es.udc.fic.corpuslab.modules.project.annotation.dtos.SaveProjectAnnotationStepRequestDto;
import es.udc.fic.corpuslab.modules.project.annotation.dtos.SaveProjectAnnotationStepResponseDto;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projects")
public class ProjectAnnotationController {

    private final ProjectAnnotationService projectAnnotationService;

    public ProjectAnnotationController(ProjectAnnotationService projectAnnotationService) {
        this.projectAnnotationService = projectAnnotationService;
    }

    @GetMapping("/{projectId}/annotations/steps")
    @ResponseStatus(HttpStatus.OK)
    public ProjectAnnotationWorkspaceDto getAnnotationWorkspace(
            Authentication authentication,
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return projectAnnotationService.getAnnotationWorkspace(authentication.getName(), projectId, offset, limit);
    }

    @GetMapping("/{projectId}/annotations/participants/{participantUserId}/steps")
    @ResponseStatus(HttpStatus.OK)
    public ProjectAnnotationWorkspaceDto getParticipantAnnotationWorkspaceForCreator(
            Authentication authentication,
            @PathVariable Long projectId,
            @PathVariable Long participantUserId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return projectAnnotationService.getParticipantAnnotationWorkspaceForCreator(
                authentication.getName(),
                projectId,
                participantUserId,
                offset,
                limit);
    }

    @PutMapping("/{projectId}/annotations/steps")
    @ResponseStatus(HttpStatus.OK)
    public SaveProjectAnnotationStepResponseDto saveAnnotationStep(
            Authentication authentication,
            @PathVariable Long projectId,
            @Valid @RequestBody SaveProjectAnnotationStepRequestDto request) {
        return projectAnnotationService.saveAnnotationStep(authentication.getName(), projectId, request);
    }

    public record ToggleAnnotationWarningRequest(Long datasetItemId, Integer stepIndex) {
    }

    public record ResolveAnnotationWarningRequest(Long datasetItemId, Integer stepIndex) {
    }

    @PutMapping("/{projectId}/annotations/participants/{participantUserId}/steps/warning")
    @ResponseStatus(HttpStatus.OK)
    public SaveProjectAnnotationStepResponseDto toggleAnnotationWarning(
            Authentication authentication,
            @PathVariable Long projectId,
            @PathVariable Long participantUserId,
            @Valid @RequestBody ToggleAnnotationWarningRequest request) {
        return projectAnnotationService.toggleAnnotationWarning(
                authentication.getName(),
                projectId,
                participantUserId,
                request.datasetItemId(),
                request.stepIndex());
    }

    @PutMapping("/{projectId}/annotations/steps/warning-resolution")
    @ResponseStatus(HttpStatus.OK)
    public SaveProjectAnnotationStepResponseDto resolveOwnAnnotationWarning(
            Authentication authentication,
            @PathVariable Long projectId,
            @Valid @RequestBody ResolveAnnotationWarningRequest request) {
        return projectAnnotationService.resolveOwnAnnotationWarning(
                authentication.getName(),
                projectId,
                request.datasetItemId(),
                request.stepIndex());
    }
}
