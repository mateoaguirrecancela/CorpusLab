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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationSourceContentDto;
import es.udc.fic.corpuslab.modules.project.dtos.UploadProjectDatasetResponseDto;
import es.udc.fic.corpuslab.modules.project.services.ProjectDatasetItemService;

@RestController
@RequestMapping("/api")
public class ProjectDatasetItemController {

    private final ProjectDatasetItemService projectDatasetItemService;

    public ProjectDatasetItemController(ProjectDatasetItemService projectDatasetItemService) {
        this.projectDatasetItemService = projectDatasetItemService;
    }

    @PostMapping(path = "/research-groups/{groupId}/projects/{projectId}/dataset", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadProjectDatasetResponseDto uploadDataset(
            Authentication authentication,
            @PathVariable Long groupId,
            @PathVariable Long projectId,
            @RequestParam("files") List<MultipartFile> files) {
        return projectDatasetItemService.uploadDataset(authentication.getName(), groupId, projectId, files);
    }

    @GetMapping("/projects/{projectId}/dataset-items/{datasetItemId}/content")
    public ResponseEntity<byte[]> getAnnotationSourceContent(
            Authentication authentication,
            @PathVariable Long projectId,
            @PathVariable Long datasetItemId) {
        ProjectAnnotationSourceContentDto sourceContent = projectDatasetItemService
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
}
