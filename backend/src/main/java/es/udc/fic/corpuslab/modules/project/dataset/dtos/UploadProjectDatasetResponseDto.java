package es.udc.fic.corpuslab.modules.project.dataset.dtos;

import java.util.List;

public record UploadProjectDatasetResponseDto(
        Long projectId,
        int uploadedItems,
        List<DatasetItemDto> items) {
}
