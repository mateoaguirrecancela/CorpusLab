package es.udc.fic.corpuslab.modules.project.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.shared.entities.Label;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectAnnotationException;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectConstants;

class AnnotationPayloadNormalizerTest {

    private final AnnotationPayloadNormalizer normalizer = new AnnotationPayloadNormalizer();

    @Test
    void normalizeShouldAcceptConfiguredSingleLabelAndTrimNotes() {
        Project project = project(ProjectType.TEXT_CLASSIFICATION_SIMPLE, "Positive", "Negative");

        Map<String, Object> normalized = normalizeMap(
                Map.of(
                        "label", " positive ",
                        "notes", " reviewed "),
                project);

        assertThat(normalized)
                .containsEntry(ProjectConstants.ANNOTATION_KEY_LABEL, "Positive")
                .containsEntry(ProjectConstants.ANNOTATION_KEY_NOTES, "reviewed");
    }

    @Test
    void normalizeShouldRejectSingleLabelOutsideProjectCatalog() {
        Project project = project(ProjectType.TEXT_CLASSIFICATION_SIMPLE, "Positive", "Negative");

        assertThatThrownBy(() -> normalizer.normalize(Map.of("label", "Other"), project))
                .isInstanceOf(InvalidProjectAnnotationException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void normalizeShouldRejectUnsupportedClassificationFields() {
        Project project = project(ProjectType.TEXT_CLASSIFICATION_SIMPLE, "Positive", "Negative");

        assertThatThrownBy(() -> normalizer.normalize(
                Map.of(
                        "label", "Positive",
                        "metadata", Map.of("arbitrary", true)),
                project))
                .isInstanceOf(InvalidProjectAnnotationException.class)
                .hasMessageContaining("unsupported fields");
    }

    @Test
    void normalizeShouldAcceptConfiguredMultilabelList() {
        Project project = project(ProjectType.TEXT_CLASSIFICATION_MULTILABEL, "Positive", "Negative", "Neutral");

        Map<String, Object> normalized = normalizeMap(
                Map.of("labels", List.of(" negative ", "POSITIVE")),
                project);

        assertThat(normalized.get(ProjectConstants.ANNOTATION_KEY_LABELS))
                .isEqualTo(List.of("Negative", "Positive"));
    }

    @Test
    void normalizeShouldRejectMultilabelPayloadWhenLabelsAreNotAList() {
        Project project = project(ProjectType.TEXT_CLASSIFICATION_MULTILABEL, "Positive", "Negative");

        assertThatThrownBy(() -> normalizer.normalize(Map.of("labels", "Positive"), project))
                .isInstanceOf(InvalidProjectAnnotationException.class)
                .hasMessageContaining("labels as a list");
    }

    @Test
    void normalizeShouldAcceptSeq2SeqTextShapeOnly() {
        Project project = project(ProjectType.SEQ2SEQ);

        Map<String, Object> normalized = normalizeMap(
                Map.of(
                        "text", " generated answer ",
                        "notes", " note "),
                project);

        assertThat(normalized)
                .containsEntry(ProjectConstants.ANNOTATION_KEY_TEXT, "generated answer")
                .containsEntry(ProjectConstants.ANNOTATION_KEY_NOTES, "note");
    }

    @Test
    void normalizeShouldRejectSeq2SeqUnsupportedFields() {
        Project project = project(ProjectType.SEQ2SEQ);

        assertThatThrownBy(() -> normalizer.normalize(
                Map.of(
                        "text", "generated answer",
                        "extra", Map.of("arbitrary", true)),
                project))
                .isInstanceOf(InvalidProjectAnnotationException.class)
                .hasMessageContaining("unsupported fields");
    }

    @Test
    void normalizeShouldRejectNerEntityLabelOutsideProjectCatalog() {
        Project project = project(ProjectType.NER, "PERSON");

        assertThatThrownBy(() -> normalizer.normalize(
                Map.of("entities", List.of(Map.of(
                        "label", "ORG",
                        "text", "OpenAI",
                        "startOffset", 0,
                        "endOffset", 6))),
                project))
                .isInstanceOf(InvalidProjectAnnotationException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void normalizeShouldRejectNerPayloadWithUnsupportedTopLevelFields() {
        Project project = project(ProjectType.NER, "PERSON");

        assertThatThrownBy(() -> normalizer.normalize(
                Map.of(
                        "entities", List.of(Map.of(
                                "label", "PERSON",
                                "text", "John",
                                "startOffset", 0,
                                "endOffset", 4)),
                        "metadata", Map.of("arbitrary", true)),
                project))
                .isInstanceOf(InvalidProjectAnnotationException.class)
                .hasMessageContaining("unsupported fields");
    }

    @Test
    void normalizeShouldRejectNerEntitiesWithUnsupportedFields() {
        Project project = project(ProjectType.NER, "PERSON");

        assertThatThrownBy(() -> normalizer.normalize(
                Map.of("entities", List.of(Map.of(
                        "label", "PERSON",
                        "text", "John",
                        "startOffset", 0,
                        "endOffset", 4,
                        "extra", "value"))),
                project))
                .isInstanceOf(InvalidProjectAnnotationException.class)
                .hasMessageContaining("unsupported fields");
    }

    @Test
    void normalizeShouldRejectNerNotesWhenNotText() {
        Project project = project(ProjectType.NER, "PERSON");

        assertThatThrownBy(() -> normalizer.normalize(
                Map.of(
                        "entities", List.of(Map.of(
                                "label", "PERSON",
                                "text", "John",
                                "startOffset", 0,
                                "endOffset", 4)),
                        "notes", Map.of("invalid", true)),
                project))
                .isInstanceOf(InvalidProjectAnnotationException.class)
                .hasMessageContaining("notes");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeMap(Object annotationPayload, Project project) {
        return (Map<String, Object>) normalizer.normalize(annotationPayload, project);
    }

    private Project project(ProjectType projectType, String... labelNames) {
        Project project = new Project();
        project.setProjectType(projectType);
        for (String labelName : labelNames) {
            Label label = new Label();
            label.setProject(project);
            label.setName(labelName);
            project.getLabels().add(label);
        }
        return project;
    }
}
