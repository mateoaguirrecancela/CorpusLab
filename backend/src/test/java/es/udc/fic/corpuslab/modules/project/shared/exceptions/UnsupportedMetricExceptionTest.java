package es.udc.fic.corpuslab.modules.project.shared.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;

class UnsupportedMetricExceptionTest {

    @Test
    void incompatibleShouldExposeProjectAndMetricTypeWithDescriptiveMessage() {
        UnsupportedMetricException exception = UnsupportedMetricException.incompatible(
                ProjectType.SEQ2SEQ, MetricType.COHENS_KAPPA);

        assertThat(exception.getProjectType()).isEqualTo(ProjectType.SEQ2SEQ);
        assertThat(exception.getMetricType()).isEqualTo(MetricType.COHENS_KAPPA);
        assertThat(exception.getMessage())
                .isEqualTo("Metric COHENS_KAPPA is not compatible with project type SEQ2SEQ");
    }

    @Test
    void notRegisteredShouldExposeProjectAndMetricTypeWithDescriptiveMessage() {
        UnsupportedMetricException exception = UnsupportedMetricException.notRegistered(
                ProjectType.NER, MetricType.SPAN_OVERLAP_F1);

        assertThat(exception.getProjectType()).isEqualTo(ProjectType.NER);
        assertThat(exception.getMetricType()).isEqualTo(MetricType.SPAN_OVERLAP_F1);
        assertThat(exception.getMessage())
                .isEqualTo("Metric SPAN_OVERLAP_F1 is compatible with project type NER "
                        + "but no calculator strategy is registered");
    }
}
