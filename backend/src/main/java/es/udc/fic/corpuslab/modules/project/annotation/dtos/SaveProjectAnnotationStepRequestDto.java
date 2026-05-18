package es.udc.fic.corpuslab.modules.project.annotation.dtos;

import jakarta.validation.constraints.NotNull;

public record SaveProjectAnnotationStepRequestDto(
                @NotNull Long datasetItemId,
                Integer stepIndex,
                Object annotation) {
}
