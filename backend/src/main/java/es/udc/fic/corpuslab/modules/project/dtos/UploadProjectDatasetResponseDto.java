package es.udc.fic.corpuslab.modules.project.dtos;

import java.util.List;

public record UploadProjectDatasetResponseDto(
        Long projectId,
        int uploadedItems,
        List<DatasetItemDto> items) {
}
