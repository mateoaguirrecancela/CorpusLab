package es.udc.fic.corpuslab.modules.project.setup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectSetupException;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectCommonUtils;

@Component
public class LabelValidator {

    public List<ProjectSetupLabelDto> normalizeLabels(List<ProjectSetupLabelDto> labels) {
        if (labels == null || labels.isEmpty()) {
            return List.of();
        }

        List<ProjectSetupLabelDto> normalizedLabels = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (ProjectSetupLabelDto label : labels) {
            if (label == null) {
                continue;
            }

            String normalizedName = StringUtils.trimToNull(label.name());
            if (normalizedName == null) {
                continue;
            }

            if (!seen.add(normalizedName.toLowerCase())) {
                throw new InvalidProjectSetupException("Duplicated labels are not allowed");
            }

            normalizedLabels.add(new ProjectSetupLabelDto(
                    normalizedName,
                    ProjectCommonUtils.normalizeHexColor(label.color())));
        }

        return normalizedLabels;
    }

    public void validateForProjectType(ProjectType projectType, List<ProjectSetupLabelDto> labels) {
        if (projectType == ProjectType.SEQ2SEQ && !labels.isEmpty()) {
            throw new InvalidProjectSetupException("Seq2Seq projects do not allow labels");
        }

        if (projectType != ProjectType.SEQ2SEQ && labels.isEmpty()) {
            throw new InvalidProjectSetupException("At least one label is required for this project type");
        }

        if ((projectType == ProjectType.TEXT_CLASSIFICATION_SIMPLE
                || projectType == ProjectType.TEXT_CLASSIFICATION_MULTILABEL) && labels.size() < 2) {
            throw new InvalidProjectSetupException("Classification projects require at least 2 labels");
        }

        if (projectType == ProjectType.NER && labels.stream().anyMatch(label -> label.color() == null)) {
            throw new InvalidProjectSetupException("NER labels require a color");
        }
    }
}
