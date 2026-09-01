package es.udc.fic.corpuslab.modules.project.dataset.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectConstants;

class TextDatasetParserTest {

    private final TextDatasetParser parser = new TextDatasetParser();

    @Test
    void readTextShouldReturnDirectTextWhenPresent() {
        DatasetItem datasetItem = new DatasetItem();
        datasetItem.setContent(Map.of("text", "Direct text content"));

        assertThat(parser.readText(datasetItem)).isEqualTo("Direct text content");
    }

    @Test
    void readTextShouldDecodeBase64WhenDirectTextIsMissing() {
        DatasetItem datasetItem = new DatasetItem();
        String encoded = Base64.getEncoder().encodeToString("Encoded content".getBytes(StandardCharsets.UTF_8));
        datasetItem.setContent(Map.of(ProjectConstants.CONTENT_KEY_BASE64, encoded));

        assertThat(parser.readText(datasetItem)).isEqualTo("Encoded content");
    }

    @Test
    void readTextShouldReturnEmptyStringWhenNeitherTextNorBase64ArePresent() {
        DatasetItem datasetItem = new DatasetItem();
        datasetItem.setContent(Map.of());

        assertThat(parser.readText(datasetItem)).isEqualTo("");
    }
}
