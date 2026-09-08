package es.udc.fic.corpuslab.modules.project.guideline;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.udc.fic.corpuslab.modules.project.guideline.dtos.ProjectGuidelinePdfContentDto;

@RestController
@RequestMapping("/api/projects")
public class ProjectGuidelineController {

    private final ProjectGuidelineService projectGuidelineService;

    public ProjectGuidelineController(ProjectGuidelineService projectGuidelineService) {
        this.projectGuidelineService = projectGuidelineService;
    }

    @GetMapping("/{projectId}/guideline-pdf")
    public ResponseEntity<byte[]> getProjectGuidelinePdf(
            Authentication authentication,
            @PathVariable Long projectId) {
        ProjectGuidelinePdfContentDto content = projectGuidelineService.getProjectGuidelinePdf(
                authentication.getName(),
                projectId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.mimeType()))
                .contentLength(content.bytes().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition
                        .inline()
                        .filename(content.fileName())
                        .build()
                        .toString())
                .body(content.bytes());
    }
}
