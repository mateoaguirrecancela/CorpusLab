package es.udc.fic.corpuslab.modules.project.dtos;

import jakarta.validation.constraints.NotNull;

public record SaveProjectAnnotationStepRequestDto(
                @NotNull Long datasetItemId,
                Integer stepIndex,
                Object annotation) {
}
