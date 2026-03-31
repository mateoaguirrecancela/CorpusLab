package es.udc.fic.corpuslab.modules.researchgroup.dtos;

import java.time.Instant;

import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;

public record ResearchGroupSummaryDto(
        Long id,
        String name,
        String description,
        ResearchGroupMemberRole role,
        long memberCount,
        Instant createdAt
) {
}
