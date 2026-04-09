package es.udc.fic.corpuslab.modules.project.dtos;

import java.time.Instant;
import java.util.List;

import es.udc.fic.corpuslab.modules.project.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;

public record ProjectDetailDto(
        Long id,
        Long researchGroupId,
        String researchGroupName,
        String name,
        String description,
        ProjectType projectType,
        boolean setupCompleted,
        ProjectParticipantRole participantRole,
        List<ProjectSetupLabelDto> labels,
        String guidelineText,
        String guidelinePdfBase64,
        long datasetItemsCount,
        Instant createdAt) {
}
