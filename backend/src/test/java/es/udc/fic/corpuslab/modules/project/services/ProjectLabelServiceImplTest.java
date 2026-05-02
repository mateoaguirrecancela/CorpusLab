package es.udc.fic.corpuslab.modules.project.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.udc.fic.corpuslab.modules.project.dtos.ProjectSetupLabelDto;
import es.udc.fic.corpuslab.modules.project.entities.Project;
import es.udc.fic.corpuslab.modules.project.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.exceptions.InvalidProjectSetupException;
import es.udc.fic.corpuslab.modules.project.repositories.LabelRepository;
import es.udc.fic.corpuslab.modules.project.repositories.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class ProjectLabelServiceImplTest {

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private ProjectRepository projectRepository;

    private ProjectLabelService projectLabelService;

    @BeforeEach
    void setUp() {
        projectLabelService = new ProjectLabelServiceImpl(labelRepository, projectRepository);
    }

    @Test
    void replaceProjectLabels_ShouldSaveNormalizedLabels_WhenInputIsValidForClassification() {
        when(projectRepository.getReferenceById(1L)).thenReturn(new Project());

        List<ProjectSetupLabelDto> inputLabels = List.of(
                new ProjectSetupLabelDto("  Label 1  ", "ff0000"), // Will normalize to uppercase #FF0000
                new ProjectSetupLabelDto("Label 2", null) // Will assign random color
        );

        List<ProjectSetupLabelDto> result = projectLabelService.replaceProjectLabels(1L, ProjectType.TEXT_CLASSIFICATION_SIMPLE, inputLabels);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Label 1");
        assertThat(result.get(0).color()).isEqualTo("FF0000");

        assertThat(result.get(1).name()).isEqualTo("Label 2");
        assertThat(result.get(1).color()).isNull();

        verify(labelRepository).deleteByProjectId(1L);
        verify(labelRepository).saveAll(any());
    }

    @Test
    void replaceProjectLabels_ShouldThrowException_WhenDuplicatedLabelsProvided() {
        List<ProjectSetupLabelDto> inputLabels = List.of(
                new ProjectSetupLabelDto("LABEL", "#FF0000"),
                new ProjectSetupLabelDto("label", "#00FF00") // Case-insensitive duplicate
        );

        assertThatThrownBy(() -> projectLabelService.replaceProjectLabels(1L, ProjectType.TEXT_CLASSIFICATION_SIMPLE, inputLabels))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("Duplicated labels");
    }

    @Test
    void replaceProjectLabels_ShouldThrowException_WhenNoLabelsProvidedForClassification() {
        List<ProjectSetupLabelDto> inputLabels = List.of();

        assertThatThrownBy(() -> projectLabelService.replaceProjectLabels(1L, ProjectType.TEXT_CLASSIFICATION_SIMPLE, inputLabels))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("At least one label is required");
    }

    @Test
    void replaceProjectLabels_ShouldThrowException_WhenLabelsProvidedForSeq2Seq() {
        List<ProjectSetupLabelDto> inputLabels = List.of(new ProjectSetupLabelDto("Label", "#FFF"));

        assertThatThrownBy(() -> projectLabelService.replaceProjectLabels(1L, ProjectType.SEQ2SEQ, inputLabels))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("Seq2Seq projects do not allow labels");
    }

    @Test
    void replaceProjectLabels_ShouldSaveEmptyList_WhenNoLabelsProvidedForSeq2Seq() {
        List<ProjectSetupLabelDto> inputLabels = List.of();

        List<ProjectSetupLabelDto> result = projectLabelService.replaceProjectLabels(1L, ProjectType.SEQ2SEQ, inputLabels);

        assertThat(result).isEmpty();
        verify(labelRepository).deleteByProjectId(1L);
    }
}
