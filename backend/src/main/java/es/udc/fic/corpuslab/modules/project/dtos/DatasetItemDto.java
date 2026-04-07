package es.udc.fic.corpuslab.modules.project.dtos;

import java.time.Instant;

public record DatasetItemDto(
        Long id,
        Integer index,
        String fileName,
        String mimeType,
        long sizeBytes,
        Instant createdAt) {
}
