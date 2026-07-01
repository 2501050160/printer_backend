package com.saipraveen.login_registration.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(24 * 60 * 60 * 1000L); // 24 hours
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError((e) -> removeEmitter(userId, emitter));
        
        try {
            emitter.send(SseEmitter.event().name("connect").data("Connected"));
        } catch (IOException e) {
            removeEmitter(userId, emitter);
        }
        return emitter;
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    public void sendProgress(Long userId, String orderId, String status, String progress) {
        if (userId == null) return;
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            Map<String, String> data = Map.of(
                "orderId", orderId,
                "status", status,
                "progress", progress != null ? progress : ""
            );
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().name("progress").data(data));
                } catch (IOException e) {
                    removeEmitter(userId, emitter);
                }
            }
        }
    }

    public void sendQueueUpdate(Long userId) {
        if (userId == null) return;
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().name("queue-update").data("update"));
                } catch (IOException e) {
                    removeEmitter(userId, emitter);
                }
            }
        }
    }
}
