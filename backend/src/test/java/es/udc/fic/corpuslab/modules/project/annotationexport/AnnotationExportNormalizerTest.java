package es.udc.fic.corpuslab.modules.project.annotationexport;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectConstants;

class AnnotationExportNormalizerTest {

    private final AnnotationExportNormalizer normalizer = new AnnotationExportNormalizer();

    @Test
    void extractCommentValueShouldReturnEmptyForNonMapPayload() {
        assertThat(normalizer.extractCommentValue("free text")).isEmpty();
        assertThat(normalizer.extractCommentValue(null)).isEmpty();
    }

    @Test
    void extractCommentValueShouldReturnEmptyWhenNotesAreMissingOrBlank() {
        assertThat(normalizer.extractCommentValue(Map.of("label", "A"))).isEmpty();
        assertThat(normalizer.extractCommentValue(Map.of(ProjectConstants.ANNOTATION_KEY_NOTES, "   "))).isEmpty();
    }

    @Test
    void extractCommentValueShouldTrimNotes() {
        assertThat(normalizer.extractCommentValue(Map.of(ProjectConstants.ANNOTATION_KEY_NOTES, "  reviewed  ")))
                .isEqualTo("reviewed");
    }

    @Test
    void normalizeListLikeValueShouldHandleNullStringAndUnsupportedTypes() {
        assertThat(normalizer.normalizeListLikeValue(null)).isEmpty();
        assertThat(normalizer.normalizeListLikeValue("   ")).isEmpty();
        assertThat(normalizer.normalizeListLikeValue(" positive ")).isEqualTo("positive");
        assertThat(normalizer.normalizeListLikeValue(42)).isEmpty();
    }

    @Test
    void normalizeListLikeValueShouldJoinDeduplicatedTrimmedEntries() {
        List<Object> values = Arrays.asList("  A ", null, "B", "A", "");

        assertThat(normalizer.normalizeListLikeValue(values)).isEqualTo("A|B");
    }
}
