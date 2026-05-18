package es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.ner;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.pairwise.AnnotatorAnnotationVector;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.pairwise.PairwiseAnnotationData;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.AnnotationUnitKey;

public record NerSpanAnnotationData(
        ProjectType projectType,
        List<AnnotatorAnnotationVector<AnnotationUnitKey, NerSpanAnnotationUnit>> annotatorVectors)
        implements PairwiseAnnotationData<AnnotationUnitKey, NerSpanAnnotationUnit> {

    public NerSpanAnnotationData {
        annotatorVectors = annotatorVectors == null ? List.of() : List.copyOf(annotatorVectors);
    }
}
