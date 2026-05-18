package es.udc.fic.corpuslab.modules.project.iaa.domain.transformers;

import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;

public interface TransformedAnnotationData {

    ProjectType projectType();

    int annotatorCount();

    int unitCount();
}
