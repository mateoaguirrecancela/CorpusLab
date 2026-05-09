package es.udc.fic.corpuslab.modules.project.iaa.calculators;

import java.util.Set;

import es.udc.fic.corpuslab.modules.project.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.context.AnnotationCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.dtos.IaaResult;

public interface IaaMetricCalculator {

    MetricType metricType();

    Set<ProjectType> supportedProjectTypes();

    IaaResult calculate(AnnotationCalculationContext context);

    default boolean supports(ProjectType projectType) {
        return supportedProjectTypes().contains(projectType);
    }
}
