package es.udc.fic.corpuslab.modules.project.guideline.dtos;

public record ProjectGuidelinePdfContentDto(
        String fileName,
        String mimeType,
        byte[] bytes) {
}
