package es.udc.fic.corpuslab.modules.project.iaa.application.port.in;

import java.util.Objects;

import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaCalculationContext;

public record CalculateIaaMetricsCommand(IaaCalculationContext context) {

    public CalculateIaaMetricsCommand {
        Objects.requireNonNull(context, "context is required");
    }
}
