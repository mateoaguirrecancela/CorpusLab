package es.udc.fic.corpuslab.modules.project.setup;

import java.util.List;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectSetupException;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectDatasetUtils;

@Component
public class NerDatasetCompatibilityValidator {

    public void validate(List<DatasetItem> datasetItems) {
        if (datasetItems.isEmpty()) {
            throw new InvalidProjectSetupException(
                    "NER projects require at least one dataset file in text, JSON or CSV format");
        }

        List<String> unsupportedFiles = datasetItems.stream()
                .filter(datasetItem -> !ProjectDatasetUtils.isNerCompatibleDatasetItem(datasetItem))
                .map(ProjectDatasetUtils::describeDatasetItem)
                .limit(5)
                .toList();

        if (!unsupportedFiles.isEmpty()) {
            throw new InvalidProjectSetupException(
                    "NER projects only support text, JSON or CSV files. Unsupported files: "
                            + String.join(", ", unsupportedFiles));
        }
    }
}
