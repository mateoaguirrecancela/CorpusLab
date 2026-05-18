package es.udc.fic.corpuslab.modules.project.iaa.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import es.udc.fic.corpuslab.modules.project.shared.enums.IaaResultStatus;
import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;

class IaaResultTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void notCalculableShouldExposeNaNValueAndStatus() {
        IaaResult result = IaaResult.notCalculable(
                MetricType.COHENS_KAPPA,
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                IaaResultStatus.NO_VALID_PAIRS,
                "No annotator pairs share annotated units",
                2,
                4,
                0,
                Map.of("ignoredPairs", 1));

        assertThat(result.calculable()).isFalse();
        assertThat(result.value()).isNaN();
        assertThat(result.status()).isEqualTo(IaaResultStatus.NO_VALID_PAIRS);
        assertThat(result.details()).containsEntry("ignoredPairs", 1);
    }

    @Test
    void calculableShouldNormalizeNonFiniteValuesAsUndefined() {
        IaaResult result = IaaResult.calculable(
                MetricType.KRIPPENDORFFS_ALPHA,
                ProjectType.TEXT_CLASSIFICATION_MULTILABEL,
                Double.POSITIVE_INFINITY,
                3,
                10,
                3,
                Map.of());

        assertThat(result.calculable()).isFalse();
        assertThat(result.value()).isNaN();
        assertThat(result.status()).isEqualTo(IaaResultStatus.UNDEFINED);
    }

    @Test
    void notCalculableShouldRejectCalculableStatus() {
        assertThatThrownBy(() -> IaaResult.notCalculable(
                MetricType.XRR,
                ProjectType.NER,
                IaaResultStatus.CALCULABLE,
                "Invalid status",
                0,
                0,
                0,
                Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void notCalculableShouldSerializeSafelyForApiResponses() {
        IaaResult result = IaaResult.notCalculable(
                MetricType.XRR,
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                IaaResultStatus.UNDEFINED,
                "XRR cannot be normalized",
                2,
                10,
                0,
                Map.of());

        assertThatCode(() -> objectMapper.writeValueAsString(result)).doesNotThrowAnyException();
    }
}
