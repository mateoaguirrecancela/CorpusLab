package es.udc.fic.corpuslab.modules.project.dataset;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import es.udc.fic.corpuslab.modules.project.dataset.dtos.ProjectDatasetUploadEventDto;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.ProjectNotFoundException;

@Service
public class ProjectDatasetUploadEvents {

    private static final long SSE_TIMEOUT_MILLIS = 10 * 60 * 1000L;

    private final ProjectDatasetUploadStatusStore statusStore;
    private final ConcurrentHashMap<String, Set<SseEmitter>> emittersByJob = new ConcurrentHashMap<>();

    public ProjectDatasetUploadEvents(ProjectDatasetUploadStatusStore statusStore) {
        this.statusStore = statusStore;
    }

    public SseEmitter open(String authenticatedEmail, Long projectId, String jobId) {
        String ownerEmail = statusStore.findOwnerEmail(jobId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        Long storedProjectId = statusStore.findProjectId(jobId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (!ownerEmail.equalsIgnoreCase(authenticatedEmail) || !storedProjectId.equals(projectId)) {
            throw new AccessDeniedException("Dataset upload job does not belong to this user");
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emittersByJob.computeIfAbsent(jobId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> remove(jobId, emitter));
        emitter.onTimeout(() -> remove(jobId, emitter));
        emitter.onError(ignored -> remove(jobId, emitter));

        statusStore.findEvent(jobId).ifPresent(event -> send(jobId, emitter, event));
        return emitter;
    }

    public void publish(ProjectDatasetUploadEventDto event, String ownerEmail, Long projectId) {
        statusStore.save(event, ownerEmail, projectId);
        Set<SseEmitter> emitters = emittersByJob.get(event.jobId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            send(event.jobId(), emitter, event);
        }
    }

    private void send(String jobId, SseEmitter emitter, ProjectDatasetUploadEventDto event) {
        try {
            emitter.send(SseEmitter.event().name("dataset-upload").data(event));
            if ("COMPLETED".equals(event.status()) || "FAILED".equals(event.status())) {
                emitter.complete();
                remove(jobId, emitter);
            }
        } catch (IOException | IllegalStateException ex) {
            remove(jobId, emitter);
        }
    }

    private void remove(String jobId, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByJob.get(jobId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByJob.remove(jobId);
        }
    }
}
