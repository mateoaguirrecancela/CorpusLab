package es.udc.fic.corpuslab.modules.project.setup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectSetupException;

class LabelValidatorTest {

    private final LabelValidator validator = new LabelValidator();

    @Test
    void normalizeLabelsShouldReturnEmptyListForNullOrEmptyInput() {
        assertThat(validator.normalizeLabels(null)).isEmpty();
        assertThat(validator.normalizeLabels(List.of())).isEmpty();
    }

    @Test
    void normalizeLabelsShouldSkipNullEntriesAndBlankNames() {
        List<ProjectSetupLabelDto> labels = Arrays.asList(
                null,
                new ProjectSetupLabelDto("   ", null),
                new ProjectSetupLabelDto(" Positive ", "#ff0000"));

        List<ProjectSetupLabelDto> normalized = validator.normalizeLabels(labels);

        assertThat(normalized).hasSize(1);
        assertThat(normalized.getFirst().name()).isEqualTo("Positive");
    }

    @Test
    void normalizeLabelsShouldRejectCaseInsensitiveDuplicates() {
        List<ProjectSetupLabelDto> labels = List.of(
                new ProjectSetupLabelDto("Positive", null),
                new ProjectSetupLabelDto("positive", null));

        assertThatThrownBy(() -> validator.normalizeLabels(labels))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("Duplicated");
    }

    @Test
    void validateForProjectTypeShouldRejectLabelsForSeq2Seq() {
        assertThatThrownBy(() -> validator.validateForProjectType(
                ProjectType.SEQ2SEQ, List.of(new ProjectSetupLabelDto("A", null))))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("do not allow labels");
    }

    @Test
    void validateForProjectTypeShouldAllowSeq2SeqWithoutLabels() {
        validator.validateForProjectType(ProjectType.SEQ2SEQ, List.of());
    }

    @Test
    void validateForProjectTypeShouldRequireAtLeastOneLabelForNonSeq2Seq() {
        assertThatThrownBy(() -> validator.validateForProjectType(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE, List.of()))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("At least one label");
    }

    @Test
    void validateForProjectTypeShouldRequireAtLeastTwoLabelsForClassification() {
        assertThatThrownBy(() -> validator.validateForProjectType(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE, List.of(new ProjectSetupLabelDto("A", null))))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("at least 2 labels");

        assertThatThrownBy(() -> validator.validateForProjectType(
                ProjectType.TEXT_CLASSIFICATION_MULTILABEL, List.of(new ProjectSetupLabelDto("A", null))))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("at least 2 labels");
    }

    @Test
    void validateForProjectTypeShouldAcceptTwoLabelsForClassification() {
        validator.validateForProjectType(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                List.of(new ProjectSetupLabelDto("A", null), new ProjectSetupLabelDto("B", null)));
    }

    @Test
    void validateForProjectTypeShouldRequireColorForNerLabels() {
        assertThatThrownBy(() -> validator.validateForProjectType(
                ProjectType.NER, List.of(new ProjectSetupLabelDto("A", null))))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("require a color");
    }

    @Test
    void validateForProjectTypeShouldAcceptNerLabelsWithColor() {
        validator.validateForProjectType(
                ProjectType.NER, List.of(new ProjectSetupLabelDto("A", "#ff0000")));
    }
}
