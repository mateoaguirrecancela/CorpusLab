package es.udc.fic.corpuslab.modules.project.setup.dtos;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;

public record ProjectSetupResponseDto(
                Long projectId,
                ProjectType projectType,
                List<ProjectSetupLabelDto> labels,
                String guidelineText,
                String annotationTargetColumn,
                boolean setupCompleted) {
}
