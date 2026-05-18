package es.udc.fic.corpuslab.modules.project.iaa.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record IaaDatasetStep(
        int stepIndex,
        String preview,
        Map<String, String> rowValues,
        String sourceText) {

    public IaaDatasetStep {
        preview = preview == null ? "" : preview;
        rowValues = rowValues == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(rowValues));
        sourceText = sourceText == null ? "" : sourceText;
    }
}
