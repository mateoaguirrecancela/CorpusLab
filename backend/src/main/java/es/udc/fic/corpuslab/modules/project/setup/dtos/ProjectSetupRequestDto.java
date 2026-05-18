package es.udc.fic.corpuslab.modules.project.setup.dtos;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProjectSetupRequestDto(
        @NotNull ProjectType projectType,
        List<@Valid ProjectSetupLabelDto> labels,
        @Size(max = 5000) String guidelineText,
        @Size(max = 256) String annotationTargetColumn) {
}
