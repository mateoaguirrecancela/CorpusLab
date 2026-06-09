package es.udc.fic.corpuslab.modules.project.dataset;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectDatasetUploadStagingCleanupTest {

    @TempDir
    Path tempDir;

    @Test
    void cleanupStaleStagingDirectoriesShouldIgnoreMissingRoot() {
        Path missingRoot = tempDir.resolve("missing-root");
        ProjectDatasetUploadStagingCleanup cleanup = new ProjectDatasetUploadStagingCleanup(missingRoot.toString(), 24L);

        cleanup.cleanupStaleStagingDirectories();

        assertThat(Files.exists(missingRoot)).isFalse();
    }

    @Test
    void cleanupStaleStagingDirectoriesShouldDeleteOnlyOldDirectories() throws Exception {
        Path stagingRoot = Files.createDirectory(tempDir.resolve("staging"));
        Path oldDirectory = Files.createDirectory(stagingRoot.resolve("old-job"));
        Path freshDirectory = Files.createDirectory(stagingRoot.resolve("fresh-job"));
        Files.writeString(oldDirectory.resolve("payload.txt"), "old");
        Files.writeString(freshDirectory.resolve("payload.txt"), "fresh");

        Files.setLastModifiedTime(oldDirectory, FileTime.from(Instant.now().minus(Duration.ofHours(4))));
        Files.setLastModifiedTime(freshDirectory, FileTime.from(Instant.now().minus(Duration.ofMinutes(10))));

        ProjectDatasetUploadStagingCleanup cleanup = new ProjectDatasetUploadStagingCleanup(stagingRoot.toString(), 1L);
        cleanup.cleanupStaleStagingDirectories();

        assertThat(Files.exists(oldDirectory)).isFalse();
        assertThat(Files.exists(freshDirectory)).isTrue();
        assertThat(Files.exists(stagingRoot)).isTrue();
    }
}
