package es.udc.fic.corpuslab.modules.researchgroup.dtos;

import java.time.Instant;
import java.util.List;

public record ResearchGroupDetailDto(
        Long id,
        String name,
        String description,
        long totalMembers,
        long activeProjects,
        Instant createdAt,
        List<ResearchGroupMemberDto> members) {
}
