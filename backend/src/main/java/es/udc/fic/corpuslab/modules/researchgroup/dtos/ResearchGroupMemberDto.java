package es.udc.fic.corpuslab.modules.researchgroup.dtos;

import java.util.List;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;

public record ResearchGroupMemberDto(
        Long userId,
        String firstName,
        String lastName,
        String email,
        ResearchGroupMemberRole role,
        long activeProjectsCount) {
}
