package es.udc.fic.corpuslab.modules.project.guideline;

import org.springframework.web.multipart.MultipartFile;

import es.udc.fic.corpuslab.modules.project.guideline.dtos.ProjectGuidelinePdfContentDto;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;

public interface ProjectGuidelineService {

    ProjectGuidelinePdfContentDto getProjectGuidelinePdf(String authenticatedEmail, Long projectId);

    String normalizeAndValidateGuidelinePdfFile(MultipartFile guidelinePdfFile);

    byte[] decodeGuidelinePdfBytes(String rawGuidelinePdfBase64);

    long calculateGuidelinePdfSizeBytes(String rawGuidelinePdfBase64);

    String buildGuidelinePdfFileName(Project project);
}
