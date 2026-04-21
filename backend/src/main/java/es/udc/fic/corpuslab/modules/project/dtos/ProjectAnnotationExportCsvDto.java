package es.udc.fic.corpuslab.modules.project.dtos;

public record ProjectAnnotationExportCsvDto(
        String fileName,
        byte[] bytes) {
}
