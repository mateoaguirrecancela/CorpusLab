package es.udc.fic.corpuslab.modules.project.exceptions;

import es.udc.fic.corpuslab.modules.project.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;

public class UnsupportedMetricException extends RuntimeException {

    private final ProjectType projectType;
    private final MetricType metricType;

    private UnsupportedMetricException(ProjectType projectType, MetricType metricType, String message) {
        super(message);
        this.projectType = projectType;
        this.metricType = metricType;
    }

    public static UnsupportedMetricException incompatible(ProjectType projectType, MetricType metricType) {
        return new UnsupportedMetricException(
                projectType,
                metricType,
                "Metric " + metricType + " is not compatible with project type " + projectType);
    }

    public static UnsupportedMetricException notRegistered(ProjectType projectType, MetricType metricType) {
        return new UnsupportedMetricException(
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
