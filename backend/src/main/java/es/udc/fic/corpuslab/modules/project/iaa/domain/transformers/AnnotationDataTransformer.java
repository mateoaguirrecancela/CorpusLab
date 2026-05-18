package es.udc.fic.corpuslab.modules.project.iaa.domain.transformers;

import java.util.Set;

import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaCalculationContext;

public interface AnnotationDataTransformer<T extends TransformedAnnotationData> {

    Set<ProjectType> supportedProjectTypes();

    Set<MetricType> supportedMetricTypes();

    T transform(IaaCalculationContext context);

    default boolean supports(ProjectType projectType, MetricType metricType) {
        return supportedProjectTypes().contains(projectType)
                && supportedMetricTypes().contains(metricType);
    }
}
