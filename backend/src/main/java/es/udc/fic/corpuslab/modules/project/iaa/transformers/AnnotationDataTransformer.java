package es.udc.fic.corpuslab.modules.project.iaa.transformers;

import java.util.Set;

import es.udc.fic.corpuslab.modules.project.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.context.AnnotationCalculationContext;

public interface AnnotationDataTransformer<T extends TransformedAnnotationData> {

    Set<ProjectType> supportedProjectTypes();

    Set<MetricType> supportedMetricTypes();

    T transform(AnnotationCalculationContext context);

    default boolean supports(ProjectType projectType, MetricType metricType) {
        return supportedProjectTypes().contains(projectType)
                && supportedMetricTypes().contains(metricType);
    }
}
