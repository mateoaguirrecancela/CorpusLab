package es.udc.fic.corpuslab.modules.project.iaa.domain.exceptions;

import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;

public class UnsupportedIaaMetricException extends RuntimeException {

    private final ProjectType projectType;
    private final MetricType metricType;

    private UnsupportedIaaMetricException(ProjectType projectType, MetricType metricType, String message) {
        super(message);
        this.projectType = projectType;
        this.metricType = metricType;
    }

    public static UnsupportedIaaMetricException incompatible(ProjectType projectType, MetricType metricType) {
        return new UnsupportedIaaMetricException(
                projectType,
                metricType,
                "Metric " + metricType + " is not compatible with project type " + projectType);
    }

    public static UnsupportedIaaMetricException notRegistered(ProjectType projectType, MetricType metricType) {
        return new UnsupportedIaaMetricException(
                projectType,
                metricType,
                "Metric " + metricType + " is compatible with project type " + projectType
                        + " but no calculator strategy is registered");
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public MetricType getMetricType() {
        return metricType;
    }
}
