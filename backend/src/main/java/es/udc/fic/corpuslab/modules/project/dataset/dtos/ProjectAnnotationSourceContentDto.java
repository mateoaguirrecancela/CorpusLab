package es.udc.fic.corpuslab.modules.project.dataset.dtos;

public record ProjectAnnotationSourceContentDto(
        String fileName,
        String mimeType,
        byte[] bytes) {
}
