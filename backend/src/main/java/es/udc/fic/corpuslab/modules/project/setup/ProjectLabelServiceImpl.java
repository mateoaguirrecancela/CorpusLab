package es.udc.fic.corpuslab.modules.project.setup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.project.setup.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.shared.entities.Label;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectSetupException;
import es.udc.fic.corpuslab.modules.project.shared.repositories.LabelRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectCommonUtils;

@Service
public class ProjectLabelServiceImpl implements ProjectLabelService {

    private final LabelRepository labelRepository;
    private final ProjectRepository projectRepository;

    public ProjectLabelServiceImpl(LabelRepository labelRepository, ProjectRepository projectRepository) {
        this.labelRepository = labelRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectSetupLabelDto> getProjectLabels(Long projectId) {
        return labelRepository.findByProjectId(projectId).stream()
                .map(label -> new ProjectSetupLabelDto(label.getName(), label.getColor()))
                .toList();
    }

    @Override
    @Transactional
    public List<ProjectSetupLabelDto> replaceProjectLabels(Long projectId, ProjectType projectType,
            List<ProjectSetupLabelDto> labelsInput) {
        List<ProjectSetupLabelDto> normalizedLabels = normalizeLabels(labelsInput);

        validateLabelsForProjectType(projectType, normalizedLabels);

        labelRepository.deleteByProjectId(projectId);

        if (normalizedLabels.isEmpty()) {
            return List.of();
        }

        Project projectRef = projectRepository.getReferenceById(projectId);
        List<Label> newLabels = normalizedLabels.stream().map(dto -> {
            Label label = new Label();
            label.setProject(projectRef);
            label.setName(dto.name());
            label.setColor(dto.color());
            return label;
        }).toList();

        labelRepository.saveAll(newLabels);
        return normalizedLabels;
    }

    private List<ProjectSetupLabelDto> normalizeLabels(List<ProjectSetupLabelDto> labels) {
        if (labels == null || labels.isEmpty()) {
            return List.of();
        }

        List<ProjectSetupLabelDto> normalizedLabels = new ArrayList<>();
        Set<String> seenNames = new LinkedHashSet<>();

        for (ProjectSetupLabelDto label : labels) {
            if (label == null)
                continue;

            String trimmedName = StringUtils.trimToNull(label.name());
            if (trimmedName != null) {
                String normalizedColor = ProjectCommonUtils.normalizeHexColor(label.color());
                String lookupKey = trimmedName.toLowerCase();

                if (!seenNames.add(lookupKey)) {
                    throw new InvalidProjectSetupException("Duplicated labels are not allowed");
                }
                normalizedLabels.add(new ProjectSetupLabelDto(trimmedName, normalizedColor));
            }
        }
        return normalizedLabels;
    }

    private void validateLabelsForProjectType(ProjectType projectType, List<ProjectSetupLabelDto> labels) {
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

        if (projectType == ProjectType.NER && labels.stream().anyMatch(l -> l.color() == null)) {
            throw new InvalidProjectSetupException("NER labels require a color");
        }
    }

}
