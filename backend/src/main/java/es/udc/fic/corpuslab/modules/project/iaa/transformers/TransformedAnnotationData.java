package es.udc.fic.corpuslab.modules.project.iaa.transformers;

import es.udc.fic.corpuslab.modules.project.enums.ProjectType;

public interface TransformedAnnotationData {

    ProjectType projectType();

    int annotatorCount();

    int unitCount();
}
