package es.udc.fic.corpuslab.modules.notification.services;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificationEvents {

    private static final long SSE_TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final ConcurrentHashMap<Long, Set<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    public SseEmitter open(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emittersByUserId.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(ignored -> remove(userId, emitter));
        send(userId, emitter, "ready", "connected");
        return emitter;
    }

    public void publish(Long userId) {
        Set<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            send(userId, emitter, "notification", "changed");
        }
    }

    private void send(Long userId, SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException ex) {
            remove(userId, emitter);
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUserId.remove(userId);
        }
    }
}
