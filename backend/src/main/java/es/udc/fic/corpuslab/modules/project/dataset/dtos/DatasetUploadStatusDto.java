package es.udc.fic.corpuslab.modules.project.dataset.dtos;

public record DatasetUploadStatusDto(
        String jobId,
        String status,
        int processedFiles,
        int totalFiles,
        String message) {
}
