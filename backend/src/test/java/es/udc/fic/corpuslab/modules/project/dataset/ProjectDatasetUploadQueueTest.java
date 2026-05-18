package es.udc.fic.corpuslab.modules.project.dataset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.udc.fic.corpuslab.common.redis.RedisStreamService;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectDatasetException;

class ProjectDatasetUploadQueueTest {

    @TempDir
    private Path stagingRoot;

    @Test
    void resolveStagedPathShouldAllowFilesInsideStagingRoot() {
        ProjectDatasetUploadQueue queue = queue();
        Path stagedPath = stagingRoot.resolve("job-1").resolve("dataset.csv");

        Path resolvedPath = queue.resolveStagedPath(stagedPath.toString());

        assertThat(resolvedPath).isEqualTo(stagedPath.toAbsolutePath().normalize());
    }

    @Test
    void resolveStagedPathShouldRejectFilesOutsideStagingRoot() {
        ProjectDatasetUploadQueue queue = queue();
        Path outsidePath = stagingRoot.getParent().resolve("dataset.csv");

        assertThatThrownBy(() -> queue.resolveStagedPath(outsidePath.toString()))
                .isInstanceOf(InvalidProjectDatasetException.class);
    }

    @Test
    void readStagedFilesShouldRejectTamperedRedisPayloadPaths() throws Exception {
        ProjectDatasetUploadQueue queue = queue();
        String files = new ObjectMapper().writeValueAsString(List.of(
                new ProjectDatasetUploadQueue.StagedDatasetFile(
                        stagingRoot.getParent().resolve("evil.csv").toString(),
                        "evil.csv",
                        "text/csv",
                        "file")));

        assertThatThrownBy(() -> queue.readStagedFiles(Map.of("files", files)))
                .isInstanceOf(InvalidProjectDatasetException.class);
    }

    private ProjectDatasetUploadQueue queue() {
        return new ProjectDatasetUploadQueue(
                mock(RedisStreamService.class),
                new ObjectMapper(),
                mock(ProjectDatasetUploadEvents.class),
                stagingRoot.toString());
    }
}
