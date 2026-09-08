package es.udc.fic.corpuslab.modules.project.iaa.adapter.in.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.auth.fixtures.UserTestBuilder;
import es.udc.fic.corpuslab.modules.project.metrics.dtos.ProjectMetricsDto;
import es.udc.fic.corpuslab.modules.project.shared.entities.Annotation;
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.enums.IaaResultStatus;
import es.udc.fic.corpuslab.modules.project.shared.enums.MetricType;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantIaaGroup;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectParticipantTestBuilder;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectTestBuilder;
import es.udc.fic.corpuslab.modules.project.iaa.application.port.in.CalculateIaaMetricsCommand;
import es.udc.fic.corpuslab.modules.project.iaa.application.port.in.CalculateIaaMetricsResult;
import es.udc.fic.corpuslab.modules.project.iaa.application.port.in.CalculateIaaMetricsUseCase;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.ner.SpanOverlapUnit;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaCalculationContext;
import es.udc.fic.corpuslab.modules.project.iaa.domain.model.IaaResult;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.xrr.XrrAnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.shared.repositories.AnnotationRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectConstants;

@ExtendWith(MockitoExtension.class)
class ProjectIaaMetricsAdapterTest {

    @Mock
    private ProjectParticipantRepository projectParticipantRepository;

    @Mock
    private DatasetItemRepository datasetItemRepository;

    @Mock
    private AnnotationRepository annotationRepository;

    @Mock
    private CalculateIaaMetricsUseCase calculateIaaMetricsUseCase;

    @Test
    void calculateShouldMapProjectEntitiesToPureIaaModels() {
        Project project = ProjectTestBuilder.validProject()
                .withProjectType(ProjectType.NER)
                .build();
        setField(project, "id", 100L);
        project.setAnnotationTargetColumn("Text");

        User leftUser = userWithId(1L, "left@example.com");
        User rightUser = userWithId(2L, "right@example.com");
        ProjectParticipant left = participant(project, leftUser, ProjectParticipantIaaGroup.GROUP_A);
        ProjectParticipant right = participant(project, rightUser, ProjectParticipantIaaGroup.GROUP_B);

        DatasetItem datasetItem = csvDatasetItem(project, 50L);
        Annotation annotation = annotation(datasetItem, leftUser);
        IaaResult expectedMetric = IaaResult.notCalculable(
                MetricType.SPAN_OVERLAP_F1,
                ProjectType.NER,
                IaaResultStatus.INSUFFICIENT_ANNOTATORS,
                "stub",
                0,
                0,
                0,
                Map.of());

        when(projectParticipantRepository.findByProjectIdWithUserAndProject(100L)).thenReturn(List.of(left, right));
        when(datasetItemRepository.findByProjectIdOrderByItemIndexAsc(100L)).thenReturn(List.of(datasetItem));
        when(annotationRepository.findByProjectIdWithDatasetItemAndUser(100L)).thenReturn(List.of(annotation));
        when(calculateIaaMetricsUseCase.calculate(any()))
                .thenReturn(new CalculateIaaMetricsResult(List.of(expectedMetric)));

        ProjectIaaMetricsAdapter adapter = new ProjectIaaMetricsAdapter(
                projectParticipantRepository,
                datasetItemRepository,
                annotationRepository,
                calculateIaaMetricsUseCase);

        ProjectMetricsDto result = adapter.calculate(100L);

        assertThat(result.projectId()).isEqualTo(100L);
        assertThat(result.metrics()).containsExactly(expectedMetric);

        ArgumentCaptor<CalculateIaaMetricsCommand> commandCaptor =
                ArgumentCaptor.forClass(CalculateIaaMetricsCommand.class);
        org.mockito.Mockito.verify(calculateIaaMetricsUseCase).calculate(commandCaptor.capture());

        IaaCalculationContext context = commandCaptor.getValue().context();
        assertThat(context.projectId()).isEqualTo(100L);
        assertThat(context.projectType()).isEqualTo(ProjectType.NER);
        assertThat(context.annotators()).extracting(annotator -> annotator.userId())
                .containsExactly(1L, 2L);
        assertThat(context.annotations()).hasSize(1);
        assertThat(context.annotations().getFirst().datasetItemId()).isEqualTo(50L);
        assertThat(context.annotations().getFirst().annotatorId()).isEqualTo(1L);
        assertThat(context.datasetItems()).hasSize(1);
        assertThat(context.datasetItems().getFirst().csvDataset()).isTrue();
        assertThat(context.datasetItems().getFirst().steps()).hasSize(2);
        assertThat(context.datasetItems().getFirst().steps().getFirst().sourceText()).isEqualTo("Hello John");
        assertThat(context.datasetItems().getFirst().steps().getFirst().rowValues())
                .containsEntry("Label", "PERSON");
        assertThat(context.metadata())
                .containsEntry("annotationTargetColumn", "Text")
                .containsEntry(XrrAnnotationDataTransformer.METADATA_KEY_GROUP_X_ANNOTATOR_IDS, List.of(1L))
                .containsEntry(XrrAnnotationDataTransformer.METADATA_KEY_GROUP_Y_ANNOTATOR_IDS, List.of(2L))
                .containsEntry(SpanOverlapUnit.METADATA_KEY, SpanOverlapUnit.CHARACTER.name());
    }

    @Test
    void calculateShouldReturnEmptyMetricsWhenProjectHasNoAnnotators() {
        ProjectIaaMetricsAdapter adapter = new ProjectIaaMetricsAdapter(
                projectParticipantRepository,
                datasetItemRepository,
                annotationRepository,
                calculateIaaMetricsUseCase);
        when(projectParticipantRepository.findByProjectIdWithUserAndProject(100L)).thenReturn(List.of());

        ProjectMetricsDto result = adapter.calculate(100L);

        assertThat(result.projectId()).isEqualTo(100L);
        assertThat(result.metrics()).isEmpty();
    }

    private User userWithId(Long id, String email) {
        User user = UserTestBuilder.validUser().withEmail(email).build();
        setField(user, "id", id);
        return user;
    }

    private ProjectParticipant participant(Project project, User user, ProjectParticipantIaaGroup iaaGroup) {
        ProjectParticipant participant = ProjectParticipantTestBuilder.validParticipant()
                .withProject(project)
                .withUser(user)
                .build();
        participant.setIaaGroup(iaaGroup);
        return participant;
    }

    private DatasetItem csvDatasetItem(Project project, Long id) {
        String csv = "Text,Label\nHello John,PERSON\nBye Jane,PERSON";
        DatasetItem datasetItem = new DatasetItem();
        setField(datasetItem, "id", id);
        datasetItem.setProject(project);
        datasetItem.setItemIndex(0);
        datasetItem.setContent(Map.of(
                ProjectConstants.CONTENT_KEY_FILE_NAME, "rows.csv",
                ProjectConstants.CONTENT_KEY_MIME_TYPE, "text/csv",
                ProjectConstants.CONTENT_KEY_BASE64, Base64.getEncoder()
                        .encodeToString(csv.getBytes(StandardCharsets.UTF_8))));
        return datasetItem;
    }

    private Annotation annotation(DatasetItem datasetItem, User user) {
        Annotation annotation = new Annotation();
        annotation.setDatasetItem(datasetItem);
        annotation.setUser(user);
        annotation.setStepIndex(0);
        annotation.setPayload(Map.of("entities", List.of()));
        return annotation;
    }

    private void setField(Object target, String fieldName, Object value) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ex) {
                current = current.getSuperclass();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }
        throw new IllegalStateException("Field not found: " + fieldName);
    }
}
