package es.udc.fic.corpuslab.modules.project.dtos;

public record ProjectAnnotationSourceContentDto(
        String fileName,
        String mimeType,
        byte[] bytes) {
}
