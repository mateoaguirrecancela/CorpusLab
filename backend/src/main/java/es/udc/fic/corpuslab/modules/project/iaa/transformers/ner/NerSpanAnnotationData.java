package es.udc.fic.corpuslab.modules.project.iaa.transformers.ner;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.calculators.pairwise.AnnotatorAnnotationVector;
import es.udc.fic.corpuslab.modules.project.iaa.calculators.pairwise.PairwiseAnnotationData;
import es.udc.fic.corpuslab.modules.project.iaa.model.AnnotationUnitKey;

public record NerSpanAnnotationData(
        ProjectType projectType,
        List<AnnotatorAnnotationVector<AnnotationUnitKey, NerSpanAnnotationUnit>> annotatorVectors)
        implements PairwiseAnnotationData<AnnotationUnitKey, NerSpanAnnotationUnit> {

    public NerSpanAnnotationData {
        annotatorVectors = annotatorVectors == null ? List.of() : List.copyOf(annotatorVectors);
    }
}
