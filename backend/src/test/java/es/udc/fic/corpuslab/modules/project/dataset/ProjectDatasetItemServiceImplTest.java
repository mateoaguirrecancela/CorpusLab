package es.udc.fic.corpuslab.modules.project.dataset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import es.udc.fic.corpuslab.modules.auth.api.AuthApiService;
import es.udc.fic.corpuslab.modules.auth.api.dtos.UserInfo;
import es.udc.fic.corpuslab.modules.project.dataset.dtos.ProjectAnnotationSourceContentDto;
import es.udc.fic.corpuslab.modules.project.dataset.dtos.UploadProjectDatasetResponseDto;
import es.udc.fic.corpuslab.modules.project.metrics.ProjectMetricsCacheService;
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.entities.Project;
import es.udc.fic.corpuslab.modules.project.shared.entities.ProjectParticipant;
import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectParticipantRole;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectDatasetException;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectParticipantTestBuilder;
import es.udc.fic.corpuslab.modules.project.fixtures.ProjectTestBuilder;
import es.udc.fic.corpuslab.modules.project.shared.repositories.DatasetItemRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectParticipantRepository;
import es.udc.fic.corpuslab.modules.project.shared.repositories.ProjectRepository;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectConstants;
import es.udc.fic.corpuslab.modules.researchgroup.api.ResearchGroupApiService;
import es.udc.fic.corpuslab.modules.researchgroup.api.dtos.ResearchGroupMemberInfo;

@ExtendWith(MockitoExtension.class)
class ProjectDatasetItemServiceImplTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private DatasetItemRepository datasetItemRepository;
    @Mock private ProjectParticipantRepository projectParticipantRepository;
    @Mock private AuthApiService authApiService;
    @Mock private ResearchGroupApiService researchGroupApiService;
    @Mock private ProjectMetricsCacheService projectMetricsCacheService;

    private ProjectDatasetItemService projectDatasetItemService;

    @BeforeEach
    void setUp() {
        projectDatasetItemService = new ProjectDatasetItemServiceImpl(
                projectRepository, datasetItemRepository, projectParticipantRepository,
                authApiService, researchGroupApiService, projectMetricsCacheService, 10_485_760L);
    }

    @Test
    void uploadDataset_ShouldSaveFiles_WhenUserIsOwner() throws Exception {
        when(authApiService.findUserByEmail("owner@example.com"))
                .thenReturn(new UserInfo(1L, "owner@example.com", "Owner", "User"));

        Project project = ProjectTestBuilder.validProject().build();
        setProjectId(project, 100L);

        when(projectRepository.findByIdAndResearchGroupId(100L, 10L)).thenReturn(Optional.of(project));
        when(researchGroupApiService.findActiveMember(10L, 1L))
                .thenReturn(Optional.of(new ResearchGroupMemberInfo(1L, "OWNER")));
        when(datasetItemRepository.countByProjectId(100L)).thenReturn(0L);

        DatasetItem savedItem = new DatasetItem();
        savedItem.setItemIndex(0);
        savedItem.setContent(Map.of());
        when(datasetItemRepository.saveAll(any())).thenReturn(List.of(savedItem));

        MockMultipartFile file = new MockMultipartFile("files", "data.txt", "text/plain", "content".getBytes());

        UploadProjectDatasetResponseDto result = projectDatasetItemService.uploadDataset("owner@example.com", 10L, 100L, List.of(file));

        assertThat(result.projectId()).isEqualTo(100L);
        assertThat(result.uploadedItems()).isEqualTo(1);
        verify(datasetItemRepository).saveAll(any());
    }

    @Test
    void uploadDataset_ShouldThrowException_WhenMultipleCsvsUploaded() {
        when(authApiService.findUserByEmail(any()))
                .thenReturn(new UserInfo(1L, "owner@example.com", "Owner", "User"));
        when(projectRepository.findByIdAndResearchGroupId(any(), any())).thenReturn(Optional.of(new Project()));
        when(researchGroupApiService.findActiveMember(any(), any()))
                .thenReturn(Optional.of(new ResearchGroupMemberInfo(1L, "OWNER")));

        MockMultipartFile file1 = new MockMultipartFile("files", "1.csv", "text/csv", "a,b".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "2.csv", "text/csv", "c,d".getBytes());

        assertThatThrownBy(() -> projectDatasetItemService.uploadDataset("owner@example.com", 10L, 100L, List.of(file1, file2)))
                .isInstanceOf(InvalidProjectDatasetException.class)
                .hasMessageContaining("single file is allowed");
    }

    @Test
    void uploadDataset_ShouldThrowException_WhenUserIsNotOwnerOrAdmin() {
        when(authApiService.findUserByEmail(any()))
                .thenReturn(new UserInfo(1L, "annotator@example.com", "Ann", "User"));
        when(projectRepository.findByIdAndResearchGroupId(any(), any())).thenReturn(Optional.of(new Project()));
        when(researchGroupApiService.findActiveMember(any(), any()))
                .thenReturn(Optional.of(new ResearchGroupMemberInfo(1L, "ANNOTATOR")));

        MockMultipartFile file = new MockMultipartFile("files", "data.txt", "text/plain", "content".getBytes());

        assertThatThrownBy(() -> projectDatasetItemService.uploadDataset("annotator@example.com", 10L, 100L, List.of(file)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAnnotationSourceContent_ShouldReturnContent() {
        when(authApiService.findUserByEmail(any()))
                .thenReturn(new UserInfo(1L, "user@example.com", "Test", "User"));

        ProjectParticipant participant = ProjectParticipantTestBuilder.validParticipant()
                .withRole(ProjectParticipantRole.PARTICIPANT).build();

        when(projectParticipantRepository.findByProjectIdAndUserId(100L, 1L)).thenReturn(Optional.of(participant));

        String base64Content = Base64.getEncoder().encodeToString("Hello World".getBytes());
        DatasetItem item = new DatasetItem();
        item.setContent(Map.of(
            ProjectConstants.CONTENT_KEY_BASE64, base64Content,
            ProjectConstants.CONTENT_KEY_MIME_TYPE, "text/plain",
            ProjectConstants.CONTENT_KEY_FILE_NAME, "test.txt"
        ));

        when(datasetItemRepository.findByIdAndProjectId(50L, 100L)).thenReturn(Optional.of(item));

        ProjectAnnotationSourceContentDto result = projectDatasetItemService.getAnnotationSourceContent("user@example.com", 100L, 50L);

        assertThat(result.mimeType()).isEqualTo("text/plain");
        assertThat(result.fileName()).isEqualTo("test.txt");
        assertThat(new String(result.bytes())).isEqualTo("Hello World");
    }

    private void setProjectId(Project project, Long id) {
        try {
            java.lang.reflect.Field field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(project, id);
        } catch (Exception e) {}
    }
}
