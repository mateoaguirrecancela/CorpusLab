package es.udc.fic.corpuslab.modules.project.dataset.dtos;

public record ProjectDatasetUploadEventDto(
        String jobId,
        String status,
        int progress,
        String message,
        UploadProjectDatasetResponseDto result) {
}
