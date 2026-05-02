package es.udc.fic.corpuslab.modules.project.services;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;

public interface ProjectLabelService {

    List<ProjectSetupLabelDto> getProjectLabels(Long projectId);

    List<ProjectSetupLabelDto> replaceProjectLabels(Long projectId, ProjectType projectType,
            List<ProjectSetupLabelDto> labelsInput);
}
