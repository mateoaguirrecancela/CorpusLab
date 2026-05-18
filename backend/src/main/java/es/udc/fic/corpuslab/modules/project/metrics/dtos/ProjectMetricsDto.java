package es.udc.fic.corpuslab.modules.project.metrics.dtos;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaResult;

public record ProjectMetricsDto(
        Long projectId,
        List<IaaResult> metrics) {

    public ProjectMetricsDto {
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
}
