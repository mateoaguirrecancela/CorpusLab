package es.udc.fic.corpuslab.modules.project.dtos;

public record ProjectDatasetUploadEventDto(
        String jobId,
        String status,
        int progress,
        String message,
        UploadProjectDatasetResponseDto result) {
}
