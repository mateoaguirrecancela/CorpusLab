package es.udc.fic.corpuslab.modules.project.setup;

import java.util.List;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectSetupException;

@Component
public class ProjectSetupValidator {

    private final LabelValidator labelValidator;
    private final AnnotationTargetColumnValidator annotationTargetColumnValidator;

    public ProjectSetupValidator(
            LabelValidator labelValidator,
            AnnotationTargetColumnValidator annotationTargetColumnValidator) {
        this.labelValidator = labelValidator;
        this.annotationTargetColumnValidator = annotationTargetColumnValidator;
    }

    public void validate(
            ProjectType projectType,
            List<ProjectSetupLabelDto> labels,
            String guidelineText,
            String guidelinePdfBase64,
            String annotationTargetColumn,
            List<DatasetItem> datasetItems) {
        labelValidator.validateForProjectType(projectType, labels);
        validateGuideline(guidelineText, guidelinePdfBase64);
        annotationTargetColumnValidator.validate(datasetItems, annotationTargetColumn);
    }

    private void validateGuideline(String guidelineText, String guidelinePdfBase64) {
        String normalizedGuidelineText = StringUtils.trimToNull(guidelineText);
        String normalizedGuidelinePdf = StringUtils.trimToNull(guidelinePdfBase64);

        if (normalizedGuidelineText == null && normalizedGuidelinePdf == null) {
            throw new InvalidProjectSetupException("Provide either a guideline text or a guideline PDF");
        }

        if (normalizedGuidelineText != null && normalizedGuidelinePdf != null) {
            throw new InvalidProjectSetupException("Guideline text and guideline PDF are mutually exclusive");
        }
    }
}
