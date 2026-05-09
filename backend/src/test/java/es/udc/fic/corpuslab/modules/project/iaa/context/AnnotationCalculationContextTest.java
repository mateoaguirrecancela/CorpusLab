package es.udc.fic.corpuslab.modules.project.iaa.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectTestBuilder;

class AnnotationCalculationContextTest {

    @Test
    void constructorShouldCopyCollectionsAndExposeImmutableViews() {
        List<Annotation> annotations = new ArrayList<>(List.of(new Annotation()));
        List<DatasetItem> datasetItems = new ArrayList<>(List.of(new DatasetItem()));
        List<ProjectParticipant> annotators = new ArrayList<>(List.of(new ProjectParticipant()));
        Map<String, Object> metadata = new LinkedHashMap<>(Map.of("mode", "test"));

        AnnotationCalculationContext context = new AnnotationCalculationContext(
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

        assertThatThrownBy(() -> context.annotations().add(new Annotation()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> context.metadata().put("other", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void fromProjectShouldUseProjectIdentityAndDefaultMissingCollections() {
        Project project = ProjectTestBuilder.validProject().withProjectType(ProjectType.TEXT_CLASSIFICATION_MULTILABEL)
                .build();
        setField(project, "id", 25L);

        AnnotationCalculationContext context = AnnotationCalculationContext.fromProject(
                project,
                null,
                null,
                null,
                null);

        assertThat(context.projectId()).isEqualTo(25L);
        assertThat(context.projectType()).isEqualTo(ProjectType.TEXT_CLASSIFICATION_MULTILABEL);
        assertThat(context.annotations()).isEmpty();
        assertThat(context.datasetItems()).isEmpty();
        assertThat(context.annotators()).isEmpty();
        assertThat(context.metadata()).isEmpty();
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
