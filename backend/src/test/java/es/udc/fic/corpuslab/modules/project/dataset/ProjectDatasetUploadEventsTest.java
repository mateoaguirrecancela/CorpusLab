package es.udc.fic.corpuslab.modules.project.dataset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import es.udc.fic.corpuslab.modules.project.dataset.dtos.ProjectDatasetUploadEventDto;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.ProjectNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProjectDatasetUploadEventsTest {

    @Mock
    private ProjectDatasetUploadStatusStore statusStore;

    private ProjectDatasetUploadEvents events;

    @BeforeEach
    void setUp() {
        events = new ProjectDatasetUploadEvents(statusStore);
    }

    @Test
    void openShouldThrowNotFoundWhenOwnerIsMissing() {
        when(statusStore.findOwnerEmail("job-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> events.open("owner@example.com", 10L, "job-1"))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("10");
    }

    @Test
    void openShouldThrowAccessDeniedWhenJobDoesNotBelongToRequester() {
        when(statusStore.findOwnerEmail("job-1")).thenReturn(Optional.of("another@example.com"));
        when(statusStore.findProjectId("job-1")).thenReturn(Optional.of(10L));

        assertThatThrownBy(() -> events.open("owner@example.com", 10L, "job-1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void openShouldRegisterEmitterWhenJobOwnershipMatches() {
        when(statusStore.findOwnerEmail("job-1")).thenReturn(Optional.of("owner@example.com"));
        when(statusStore.findProjectId("job-1")).thenReturn(Optional.of(10L));
        when(statusStore.findEvent("job-1")).thenReturn(Optional.empty());

        SseEmitter emitter = events.open("owner@example.com", 10L, "job-1");

        assertThat(emitter).isNotNull();
        assertThat(emittersByJob()).containsKey("job-1");
        assertThat(emittersByJob().get("job-1")).hasSize(1);
    }

    @Test
    void publishShouldPersistStatusAndIgnoreWhenNoEmitterIsRegistered() {
        ProjectDatasetUploadEventDto event = new ProjectDatasetUploadEventDto("job-2", "RUNNING", 20, "working", null);

        events.publish(event, "owner@example.com", 33L);

        verify(statusStore).save(event, "owner@example.com", 33L);
    }

    @Test
    void publishShouldCleanupEmitterOnTerminalStatus() {
        when(statusStore.findOwnerEmail("job-3")).thenReturn(Optional.of("owner@example.com"));
        when(statusStore.findProjectId("job-3")).thenReturn(Optional.of(10L));
        when(statusStore.findEvent("job-3")).thenReturn(Optional.empty());
        events.open("owner@example.com", 10L, "job-3");
        assertThat(emittersByJob().get("job-3")).hasSize(1);

        ProjectDatasetUploadEventDto terminalEvent = new ProjectDatasetUploadEventDto("job-3", "COMPLETED", 100, "done", null);
        events.publish(terminalEvent, "owner@example.com", 10L);

        assertThat(emittersByJob()).doesNotContainKey("job-3");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Set<SseEmitter>> emittersByJob() {
        try {
            var field = ProjectDatasetUploadEvents.class.getDeclaredField("emittersByJob");
            field.setAccessible(true);
            return (ConcurrentHashMap<String, Set<SseEmitter>>) field.get(events);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
