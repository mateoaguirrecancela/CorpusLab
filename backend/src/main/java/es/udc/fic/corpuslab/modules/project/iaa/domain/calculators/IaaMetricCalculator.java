package es.udc.fic.corpuslab.modules.project.iaa.domain.calculators;

import java.util.Set;

import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaResult;

public interface IaaMetricCalculator {

    MetricType metricType();

    Set<ProjectType> supportedProjectTypes();

    IaaResult calculate(IaaCalculationContext context);

    default boolean supports(ProjectType projectType) {
        return supportedProjectTypes().contains(projectType);
    }
}
