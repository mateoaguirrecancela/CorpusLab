package es.udc.fic.corpuslab.modules.project.dtos;

import java.time.Instant;

public record ProjectSummaryDto(
        Long id,
        Long researchGroupId,
        String name,
        String description,
        Instant createdAt) {
}
