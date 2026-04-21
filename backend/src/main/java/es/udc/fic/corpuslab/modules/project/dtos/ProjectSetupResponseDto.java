package es.udc.fic.corpuslab.modules.project.dtos;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.enums.ProjectType;

public record ProjectSetupResponseDto(
                Long projectId,
                ProjectType projectType,
                List<ProjectSetupLabelDto> labels,
                String guidelineText,
                String guidelinePdfBase64,
                String annotationTargetColumn,
                boolean setupCompleted) {
}
