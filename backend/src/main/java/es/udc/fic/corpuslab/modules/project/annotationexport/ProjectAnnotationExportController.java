package es.udc.fic.corpuslab.modules.project.annotationexport;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/projects")
public class ProjectAnnotationExportController {

    private final ProjectAnnotationExportService projectAnnotationExportService;

    public ProjectAnnotationExportController(ProjectAnnotationExportService projectAnnotationExportService) {
        this.projectAnnotationExportService = projectAnnotationExportService;
    }

    @GetMapping("/{projectId}/annotations/export")
    public ResponseEntity<StreamingResponseBody> exportAnnotationResultsCsv(
            Authentication authentication,
            @PathVariable Long projectId) {
        String fileName = projectAnnotationExportService
                .getAnnotationResultsCsvFileName(authentication.getName(), projectId)
                .replace("\"", "");
        StreamingResponseBody body = outputStream -> projectAnnotationExportService
                .writeAnnotationResultsCsv(authentication.getName(), projectId, outputStream);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
                .body(body);
    }
}
