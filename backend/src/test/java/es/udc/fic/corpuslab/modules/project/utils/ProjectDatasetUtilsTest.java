package es.udc.fic.corpuslab.modules.project.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;

class ProjectDatasetUtilsTest {

    @Test
    void parseCsvDatasetContentShouldNormalizeHeadersAndKeepExtraColumns() {
        DatasetItem datasetItem = datasetItemWithId(
                200L,
                0,
                "table.csv",
                "text/csv",
                "\uFEFFName, ,name\n\"Alice, A.\",,Admin\nBob,Team Lead,Reviewer,extra");

        ProjectDatasetUtils.CsvDatasetContent csvContent = ProjectDatasetUtils.parseCsvDatasetContent(datasetItem);

        assertThat(csvContent.headers()).containsExactly("Name", "column_2", "name_2");
        assertThat(csvContent.steps()).hasSize(2);
        assertThat(csvContent.steps().getFirst().rowValues())
                .containsEntry("Name", "Alice, A.")
                .containsEntry("column_2", "")
                .containsEntry("name_2", "Admin");
        assertThat(csvContent.steps().get(1).rowValues())
                .containsEntry("column_4", "extra");
    }

    @Test
    void resolveStepDefinitionShouldReturnFallbackPreviewForNonCsvItems() {
        DatasetItem datasetItem = datasetItemWithId(201L, 1, "", "application/pdf", "unused");

        ProjectDatasetUtils.DatasetStepDefinition definition = ProjectDatasetUtils.resolveStepDefinition(datasetItem);

        assertThat(definition.totalSteps()).isEqualTo(1);
        assertThat(definition.previewForStep(0)).isEqualTo("Dataset item #2");
        assertThat(definition.rowValuesForStep(0)).isNull();
    }

    @Test
    void decodeStoredBase64ShouldHandleDataUrlPrefixes() {
        String encoded = Base64.getEncoder().encodeToString("hello".getBytes(StandardCharsets.UTF_8));

        byte[] decoded = ProjectDatasetUtils.decodeStoredBase64("data:text/plain;base64," + encoded);

        assertThat(new String(decoded, StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    private DatasetItem datasetItemWithId(Long id, int itemIndex, String fileName, String mimeType, String content) {
        DatasetItem datasetItem = new DatasetItem();
        setField(datasetItem, "id", id);
        datasetItem.setItemIndex(itemIndex);
        datasetItem.setContent(java.util.Map.of(
                ProjectConstants.CONTENT_KEY_FILE_NAME, fileName,
                ProjectConstants.CONTENT_KEY_MIME_TYPE, mimeType,
                ProjectConstants.CONTENT_KEY_BASE64, Base64.getEncoder()
                        .encodeToString(content.getBytes(StandardCharsets.UTF_8))));
        return datasetItem;
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
