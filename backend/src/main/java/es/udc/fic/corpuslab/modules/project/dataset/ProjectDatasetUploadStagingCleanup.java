package es.udc.fic.corpuslab.modules.project.dataset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class ProjectDatasetUploadStagingCleanup {

    private static final Logger logger = LoggerFactory.getLogger(ProjectDatasetUploadStagingCleanup.class);

    private final Path stagingRoot;
    private final Duration maxAge;

    public ProjectDatasetUploadStagingCleanup(
            @Value("${app.dataset-upload.staging-dir:}") String configuredStagingRoot,
            @Value("${app.dataset-upload.staging-cleanup-max-age-hours:24}") long maxAgeHours) {
        this.stagingRoot = configuredStagingRoot == null || configuredStagingRoot.isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "corpuslab-dataset-upload")
                        .toAbsolutePath()
                        .normalize()
                : Path.of(configuredStagingRoot).toAbsolutePath().normalize();
        this.maxAge = Duration.ofHours(Math.max(1L, maxAgeHours));
    }

    @Scheduled(fixedDelayString = "${app.dataset-upload.staging-cleanup-delay-ms:3600000}")
    public void cleanupStaleStagingDirectories() {
        if (!Files.isDirectory(stagingRoot)) {
            return;
        }

        Instant cutoff = Instant.now().minus(maxAge);
        try (var directories = Files.list(stagingRoot)) {
            directories
                    .filter(Files::isDirectory)
                    .filter(directory -> isOlderThan(directory, cutoff))
                    .forEach(this::deleteDirectory);
        } catch (IOException ex) {
            logger.warn("Could not scan dataset upload staging directory {}", stagingRoot, ex);
        }
    }

    private boolean isOlderThan(Path directory, Instant cutoff) {
        try {
            return Files.getLastModifiedTime(directory).toInstant().isBefore(cutoff);
        } catch (IOException ex) {
            logger.warn("Could not read last modified time for staging directory {}", directory, ex);
            return false;
        }
    }

    private void deleteDirectory(Path directory) {
        Path safeDirectory = directory.toAbsolutePath().normalize();
        if (safeDirectory.equals(stagingRoot) || !safeDirectory.startsWith(stagingRoot)) {
            logger.warn("Refusing to delete path outside dataset upload staging directory {}", directory);
            return;
        }

        try (var walk = Files.walk(safeDirectory)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            logger.warn("Could not delete stale dataset upload staging path {}", path, ex);
                        }
                    });
            logger.info("Deleted stale dataset upload staging directory {}", safeDirectory);
        } catch (IOException ex) {
            logger.warn("Could not delete stale dataset upload staging directory {}", safeDirectory, ex);
        }
    }
}
