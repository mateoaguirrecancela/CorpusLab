package es.udc.fic.corpuslab.modules.project.dataset.parser;

import java.util.List;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectDatasetUtils;

@Component
public class CsvDatasetParser {

    public ProjectDatasetUtils.CsvDatasetContent parse(DatasetItem datasetItem) {
        return ProjectDatasetUtils.parseCsvDatasetContent(datasetItem);
    }

    public List<String> headerColumns(DatasetItem datasetItem) {
        return ProjectDatasetUtils.parseCsvHeaderColumns(datasetItem);
    }
}
