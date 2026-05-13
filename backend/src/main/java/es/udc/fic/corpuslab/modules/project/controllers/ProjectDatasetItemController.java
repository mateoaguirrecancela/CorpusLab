package es.udc.fic.corpuslab.modules.project.controllers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import es.udc.fic.corpuslab.modules.project.dtos.ProjectAnnotationSourceContentDto;
import es.udc.fic.corpuslab.modules.project.dtos.ProjectDatasetUploadJobDto;
import es.udc.fic.corpuslab.modules.project.services.ProjectDatasetItemService;
import es.udc.fic.corpuslab.modules.project.services.ProjectDatasetUploadEvents;
import es.udc.fic.corpuslab.modules.project.services.ProjectDatasetUploadQueue;

@RestController
@RequestMapping("/api")
public class ProjectDatasetItemController {

    private final ProjectDatasetItemService projectDatasetItemService;
    private final ProjectDatasetUploadQueue projectDatasetUploadQueue;
    private final ProjectDatasetUploadEvents projectDatasetUploadEvents;

    public ProjectDatasetItemController(
            ProjectDatasetItemService projectDatasetItemService,
            ProjectDatasetUploadQueue projectDatasetUploadQueue,
            ProjectDatasetUploadEvents projectDatasetUploadEvents) {
        this.projectDatasetItemService = projectDatasetItemService;
        this.projectDatasetUploadQueue = projectDatasetUploadQueue;
        this.projectDatasetUploadEvents = projectDatasetUploadEvents;
    }

    @PostMapping(path = "/research-groups/{groupId}/projects/{projectId}/dataset", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ProjectDatasetUploadJobDto uploadDataset(
            Authentication authentication,
            @PathVariable Long groupId,
            @PathVariable Long projectId,
            @RequestParam("files") List<MultipartFile> files) {
        projectDatasetItemService.validateDatasetUploadRequest(authentication.getName(), groupId, projectId, files);
        String jobId = projectDatasetUploadQueue.enqueue(authentication.getName(), groupId, projectId, files);
        return new ProjectDatasetUploadJobDto(jobId);
    }

    @GetMapping(path = "/projects/{projectId}/dataset/upload-events/{jobId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getDatasetUploadEvents(
            Authentication authentication,
            @PathVariable Long projectId,
            @PathVariable String jobId) {
        return projectDatasetUploadEvents.open(authentication.getName(), projectId, jobId);
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
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8).build().toString())
                .body(sourceContent.bytes());
    }
}
