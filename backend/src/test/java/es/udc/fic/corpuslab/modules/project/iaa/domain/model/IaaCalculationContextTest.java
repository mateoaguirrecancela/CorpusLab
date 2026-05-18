package es.udc.fic.corpuslab.modules.project.iaa.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;

class IaaCalculationContextTest {

    @Test
    void constructorShouldCopyCollectionsAndExposeImmutableViews() {
        List<IaaAnnotation> annotations = new ArrayList<>(List.of(new IaaAnnotation(1L, 2L, 0, Map.of())));
        List<IaaDatasetItem> datasetItems = new ArrayList<>(
                List.of(new IaaDatasetItem(1L, 0, List.of(new IaaDatasetStep(0, "text", Map.of(), "text")), false)));
        List<IaaAnnotator> annotators = new ArrayList<>(List.of(new IaaAnnotator(2L)));
        Map<String, Object> metadata = new LinkedHashMap<>(Map.of("mode", "test"));

        IaaCalculationContext context = new IaaCalculationContext(
                8L,
                ProjectType.NER,
                annotations,
                datasetItems,
                annotators,
                metadata);

        annotations.clear();
        datasetItems.clear();
        annotators.clear();
        metadata.put("mode", "changed");

        assertThat(context.annotations()).hasSize(1);
        assertThat(context.datasetItems()).hasSize(1);
        assertThat(context.annotators()).hasSize(1);
        assertThat(context.metadata()).containsEntry("mode", "test");

        assertThatThrownBy(() -> context.annotations().add(new IaaAnnotation(2L, 3L, 0, Map.of())))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> context.metadata().put("other", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void constructorShouldExposeProjectIdentityWithExplicitEmptyCollections() {
        IaaCalculationContext context = new IaaCalculationContext(
                25L,
                ProjectType.TEXT_CLASSIFICATION_MULTILABEL,
                List.of(),
                List.of(),
                List.of(),
                Map.of());

        assertThat(context.projectId()).isEqualTo(25L);
        assertThat(context.projectType()).isEqualTo(ProjectType.TEXT_CLASSIFICATION_MULTILABEL);
        assertThat(context.annotations()).isEmpty();
        assertThat(context.datasetItems()).isEmpty();
        assertThat(context.annotators()).isEmpty();
        assertThat(context.metadata()).isEmpty();
    }
}
