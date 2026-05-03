package es.udc.fic.corpuslab.modules.project.dtos;

import java.util.Map;

public record ProjectAnnotationStepDto(
                Long datasetItemId,
                Integer datasetItemIndex,
                int stepIndex,
                int totalStepsForItem,
                String sourceName,
                String sourceMimeType,
                String preview,
                Map<String, String> rowValues,
                boolean completed,
                boolean warning,
                Object annotation) {
}
