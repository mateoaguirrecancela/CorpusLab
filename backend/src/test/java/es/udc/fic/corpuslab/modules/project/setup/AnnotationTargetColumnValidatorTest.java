package es.udc.fic.corpuslab.modules.project.setup;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectSetupException;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectConstants;

class AnnotationTargetColumnValidatorTest {

    private final AnnotationTargetColumnValidator validator = new AnnotationTargetColumnValidator();

    @Test
    void validateShouldRequireAnnotationTargetColumnWhenDatasetContainsCsvFiles() {
        List<DatasetItem> datasetItems = List.of(
                csvDatasetItem(0, "entries.csv", "text/csv", "text,label\nhello,GREETING"));

        assertThatThrownBy(() -> validator.validate(datasetItems, null))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("required");
    }

    @Test
    void validateShouldAcceptCaseInsensitiveColumnWhenPresentInAllCsvFiles() {
        List<DatasetItem> datasetItems = List.of(
                csvDatasetItem(0, "entries-a.csv", "text/csv", "Text,Label\nhello,GREETING"),
                csvDatasetItem(1, "entries-b.csv", "application/csv", "TEXT,LABEL\nbye,FAREWELL"),
                textDatasetItem(2, "notes.txt", "text/plain", "ignored"));

        assertThatCode(() -> validator.validate(datasetItems, "text"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateShouldRejectColumnNotPresentInOneCsvFile() {
        List<DatasetItem> datasetItems = List.of(
                csvDatasetItem(0, "entries-a.csv", "text/csv", "text,label\nhello,GREETING"),
                csvDatasetItem(1, "entries-b.csv", "text/csv", "content,tag\nbye,FAREWELL"));

        assertThatThrownBy(() -> validator.validate(datasetItems, "text"))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("was not found")
                .hasMessageContaining("entries-b.csv");
    }

    @Test
    void validateShouldRejectAnnotationTargetColumnWhenDatasetContainsNoCsvFiles() {
        List<DatasetItem> datasetItems = List.of(
                textDatasetItem(0, "notes.txt", "text/plain", "free text"));

        assertThatThrownBy(() -> validator.validate(datasetItems, "text"))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("can only be configured");
    }

    @Test
    void validateShouldAllowNullAnnotationTargetColumnWhenDatasetContainsNoCsvFiles() {
        List<DatasetItem> datasetItems = List.of(
                textDatasetItem(0, "notes.txt", "text/plain", "free text"));

        assertThatCode(() -> validator.validate(datasetItems, null))
                .doesNotThrowAnyException();
    }

    private DatasetItem csvDatasetItem(int itemIndex, String fileName, String mimeType, String csvContent) {
        return datasetItem(itemIndex, fileName, mimeType, csvContent);
    }

    private DatasetItem textDatasetItem(int itemIndex, String fileName, String mimeType, String textContent) {
        return datasetItem(itemIndex, fileName, mimeType, textContent);
    }

    private DatasetItem datasetItem(int itemIndex, String fileName, String mimeType, String content) {
        DatasetItem datasetItem = new DatasetItem();
        datasetItem.setItemIndex(itemIndex);
        datasetItem.setContent(Map.of(
                ProjectConstants.CONTENT_KEY_FILE_NAME, fileName,
                ProjectConstants.CONTENT_KEY_MIME_TYPE, mimeType,
                ProjectConstants.CONTENT_KEY_BASE64, Base64.getEncoder()
                        .encodeToString(content.getBytes(StandardCharsets.UTF_8))));
        return datasetItem;
    }
}
