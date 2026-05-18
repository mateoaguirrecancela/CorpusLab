package es.udc.fic.corpuslab.modules.project.core.dtos;

import java.time.Instant;
import java.util.List;

import es.udc.fic.corpuslab.modules.project.dataset.dtos.DatasetItemDto;
import es.udc.fic.corpuslab.modules.project.participant.dtos.ProjectDetailParticipantDto;
import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;

public record ProjectDetailDto(
        Long id,
        Long researchGroupId,
        String researchGroupName,
        String name,
        String description,
        ProjectType projectType,
        int completionPercentage,
        ProjectParticipantRole participantRole,
        List<ProjectDetailParticipantDto> participants,
        List<DatasetItemDto> datasetItems,
        List<ProjectSetupLabelDto> labels,
        String guidelineText,
        String guidelinePdfBase64,
        boolean guidelinePdfAvailable,
        String guidelinePdfMimeType,
        long guidelinePdfSizeBytes,
        String annotationTargetColumn,
        long datasetItemsCount,
        boolean canManageProject,
        boolean canArchiveProject,
        boolean canExportAnnotations,
        boolean archived,
        Instant createdAt) {
}
