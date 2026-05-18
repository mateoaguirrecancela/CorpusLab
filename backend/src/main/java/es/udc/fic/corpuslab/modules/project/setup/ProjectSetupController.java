package es.udc.fic.corpuslab.modules.project.setup;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupRequestDto;
import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupResponseDto;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ProjectSetupController {

    private final ProjectSetupService projectSetupService;

    public ProjectSetupController(ProjectSetupService projectSetupService) {
        this.projectSetupService = projectSetupService;
    }

    @PutMapping(
            value = "/research-groups/{groupId}/projects/{projectId}/setup",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ProjectSetupResponseDto configureProjectSetup(
            Authentication authentication,
            @PathVariable Long groupId,
            @PathVariable Long projectId,
            @Valid @RequestPart("request") ProjectSetupRequestDto request,
            @RequestPart(value = "guidelinePdfFile", required = false) MultipartFile guidelinePdfFile) {
        return projectSetupService.configureProjectSetup(
                authentication.getName(),
                groupId,
                projectId,
                request,
                guidelinePdfFile);
    }

    @PutMapping(
            value = "/research-groups/{groupId}/projects/{projectId}/setup",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ProjectSetupResponseDto configureProjectSetupJson(
            Authentication authentication,
            @PathVariable Long groupId,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectSetupRequestDto request) {
        return projectSetupService.configureProjectSetup(
                authentication.getName(),
                groupId,
                projectId,
                request,
                null);
    }
}
