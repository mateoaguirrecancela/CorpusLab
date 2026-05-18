package es.udc.fic.corpuslab.modules.project.dataset.parser;

import java.util.Map;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;

@Component
public class JsonDatasetParser {

    public Map<String, Object> content(DatasetItem datasetItem) {
        return datasetItem.getContent();
    }
}
