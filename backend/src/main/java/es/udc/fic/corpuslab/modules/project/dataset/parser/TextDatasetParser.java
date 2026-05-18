package es.udc.fic.corpuslab.modules.project.dataset.parser;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectConstants;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectDatasetUtils;

@Component
public class TextDatasetParser {

    public String readText(DatasetItem datasetItem) {
        Object directText = datasetItem.getContent().get("text");
        String text = ProjectDatasetUtils.valueAsString(directText);
        if (!text.isBlank()) {
            return text;
        }

        String base64 = ProjectDatasetUtils.valueAsString(
                datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_BASE64));
        if (base64.isBlank()) {
            return "";
        }
        return new String(ProjectDatasetUtils.decodeStoredBase64(base64), StandardCharsets.UTF_8);
    }
}
