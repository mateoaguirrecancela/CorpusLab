package es.udc.fic.corpuslab.modules.project.setup;

import static org.assertj.core.api.Assertions.assertThat;
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

class NerDatasetCompatibilityValidatorTest {

    private final NerDatasetCompatibilityValidator validator = new NerDatasetCompatibilityValidator();

    @Test
    void validateShouldRejectEmptyDataset() {
        assertThatThrownBy(() -> validator.validate(List.of()))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("require at least one dataset file");
    }

    @Test
    void validateShouldRejectUnsupportedFileTypesAndCapListedFilesToFive() {
        List<DatasetItem> datasetItems = List.of(
                datasetItem(0, "unsupported-1.pdf", "application/pdf"),
                datasetItem(1, "unsupported-2.doc", "application/msword"),
                datasetItem(2, "unsupported-3.ppt", "application/vnd.ms-powerpoint"),
                datasetItem(3, "unsupported-4.bin", "application/octet-stream"),
                datasetItem(4, "unsupported-5.dat", "application/octet-stream"),
                datasetItem(5, "unsupported-6.exe", "application/octet-stream"));

        var thrown = assertThatThrownBy(() -> validator.validate(datasetItems))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("Unsupported files")
                .hasMessageContaining("unsupported-1.pdf")
                .hasMessageContaining("unsupported-5.dat")
                .actual();

        assertThat(thrown.getMessage()).doesNotContain("unsupported-6.exe");
    }

    @Test
    void validateShouldAcceptSupportedTextJsonAndCsvFiles() {
        List<DatasetItem> datasetItems = List.of(
                datasetItem(0, "sentences.txt", "text/plain"),
                datasetItem(1, "samples.json", "application/json"),
                datasetItem(2, "rows.csv", "text/csv"));

        assertThatCode(() -> validator.validate(datasetItems))
                .doesNotThrowAnyException();
    }

    private DatasetItem datasetItem(int itemIndex, String fileName, String mimeType) {
        DatasetItem datasetItem = new DatasetItem();
        datasetItem.setItemIndex(itemIndex);
        datasetItem.setContent(Map.of(
                ProjectConstants.CONTENT_KEY_FILE_NAME, fileName,
                ProjectConstants.CONTENT_KEY_MIME_TYPE, mimeType,
                ProjectConstants.CONTENT_KEY_BASE64, Base64.getEncoder()
                        .encodeToString("content".getBytes(StandardCharsets.UTF_8))));
        return datasetItem;
    }
}
