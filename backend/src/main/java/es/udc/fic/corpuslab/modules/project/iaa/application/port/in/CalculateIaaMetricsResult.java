package es.udc.fic.corpuslab.modules.project.iaa.application.port.in;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaResult;

public record CalculateIaaMetricsResult(List<IaaResult> metrics) {

    public CalculateIaaMetricsResult {
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
}
