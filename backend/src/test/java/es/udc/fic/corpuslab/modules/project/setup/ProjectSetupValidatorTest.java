package es.udc.fic.corpuslab.modules.project.setup;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectSetupException;

@ExtendWith(MockitoExtension.class)
class ProjectSetupValidatorTest {

    @Mock
    private LabelValidator labelValidator;

    @Mock
    private AnnotationTargetColumnValidator annotationTargetColumnValidator;

    private ProjectSetupValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ProjectSetupValidator(labelValidator, annotationTargetColumnValidator);
    }

    @Test
    void validateShouldRequireEitherGuidelineTextOrGuidelinePdf() {
        List<ProjectSetupLabelDto> labels = List.of(new ProjectSetupLabelDto("Positive", "#AA00BB"));

        assertThatThrownBy(() -> validator.validate(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                labels,
                "   ",
                null,
                null,
                List.of()))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("Provide either a guideline text or a guideline PDF");

        verify(annotationTargetColumnValidator, never()).validate(List.of(), null);
    }

    @Test
    void validateShouldRejectGuidelineTextAndGuidelinePdfTogether() {
        List<ProjectSetupLabelDto> labels = List.of(new ProjectSetupLabelDto("Positive", "#AA00BB"));

        assertThatThrownBy(() -> validator.validate(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                labels,
                "Guideline text",
                "UERG",
                null,
                List.of()))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("mutually exclusive");

        verify(annotationTargetColumnValidator, never()).validate(List.of(), null);
    }

    @Test
    void validateShouldDelegateToComposedValidatorsWhenGuidelineTextIsProvided() {
        List<ProjectSetupLabelDto> labels = List.of(new ProjectSetupLabelDto("Positive", "#AA00BB"));
        List<DatasetItem> datasetItems = List.of(new DatasetItem());

        assertThatCode(() -> validator.validate(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                labels,
                "  Guideline text  ",
                null,
                "target",
                datasetItems))
                .doesNotThrowAnyException();

        verify(labelValidator).validateForProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE, labels);
        verify(annotationTargetColumnValidator).validate(datasetItems, "target");
    }

    @Test
    void validateShouldDelegateToComposedValidatorsWhenGuidelinePdfIsProvided() {
        List<ProjectSetupLabelDto> labels = List.of(new ProjectSetupLabelDto("Positive", "#AA00BB"));

        assertThatCode(() -> validator.validate(
                ProjectType.TEXT_CLASSIFICATION_SIMPLE,
                labels,
                null,
                "UERG",
                null,
                List.of()))
                .doesNotThrowAnyException();

        verify(labelValidator).validateForProjectType(ProjectType.TEXT_CLASSIFICATION_SIMPLE, labels);
        verify(annotationTargetColumnValidator).validate(List.of(), null);
    }
}
