package es.udc.fic.corpuslab.modules.project.annotation;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectDatasetException;

@Component
public class AnnotationStepResolver {

    public int resolveStepIndex(Integer requestedStepIndex, int totalSteps) {
        int stepIndex = requestedStepIndex == null ? 0 : requestedStepIndex;
        if (stepIndex < 0 || stepIndex >= totalSteps) {
            throw new InvalidProjectDatasetException("Invalid annotation step index");
        }
        return stepIndex;
    }
}
