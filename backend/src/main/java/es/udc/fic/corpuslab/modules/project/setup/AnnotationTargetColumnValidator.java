package es.udc.fic.corpuslab.modules.project.setup;

import java.util.List;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectSetupException;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectDatasetUtils;

@Component
public class AnnotationTargetColumnValidator {

    public void validate(List<DatasetItem> datasetItems, String annotationTargetColumn) {
        boolean hasCsvDataset = datasetItems.stream().anyMatch(ProjectDatasetUtils::isCsvDatasetItem);

        if (hasCsvDataset) {
            if (annotationTargetColumn == null) {
                throw new InvalidProjectSetupException(
                        "Annotation target column is required when the dataset includes CSV files");
            }
            validateColumnExistsInCsvDatasetItems(datasetItems, annotationTargetColumn);
        } else if (annotationTargetColumn != null) {
            throw new InvalidProjectSetupException(
                    "Annotation target column can only be configured when the dataset includes CSV files");
        }
    }

    private void validateColumnExistsInCsvDatasetItems(
            List<DatasetItem> datasetItems,
            String annotationTargetColumn) {
        for (DatasetItem datasetItem : datasetItems) {
            if (!ProjectDatasetUtils.isCsvDatasetItem(datasetItem)) {
                continue;
            }

            List<String> headerColumns = ProjectDatasetUtils.parseCsvHeaderColumns(datasetItem);
            boolean columnExists = headerColumns.stream()
                    .anyMatch(headerColumn -> headerColumn.equalsIgnoreCase(annotationTargetColumn));

            if (!columnExists) {
                throw new InvalidProjectSetupException(
                        "Selected annotation target column '" + annotationTargetColumn
                                + "' was not found in CSV file "
                                + ProjectDatasetUtils.describeDatasetItem(datasetItem));
            }
        }
    }
}
