package com.saipraveen.login_registration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.saipraveen.login_registration.entity.PdfFile;
import com.saipraveen.login_registration.service.QueueService;
import com.saipraveen.login_registration.service.SseService;

@RestController
@RequestMapping("/api/queue")
@CrossOrigin(origins = "http://localhost:5173")
public class QueueController {

    @Autowired
    private QueueService queueService;

    @Autowired
    private SseService sseService;

    @GetMapping("/pending")
    public ResponseEntity<?> getPending(
            @RequestParam String blockLocation
    ) {

        return ResponseEntity.ok(
                queueService.getQueueByBlock(blockLocation)
        );
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActive(
            @RequestParam String blockLocation
    ) {

        return ResponseEntity.ok(
                queueService.getActiveQueueByBlock(blockLocation)
        );
    }

    @GetMapping("/next")
    public ResponseEntity<?> getNext(
            @RequestParam String blockLocation
    ) {

        PdfFile next =
                queueService.getNextForAgent(blockLocation);

        if (next == null) {
            return ResponseEntity.ok(null);
        }

        return ResponseEntity.ok(next);
    }

    @PostMapping("/startPrinting")
    public ResponseEntity<?> startPrinting(
            @RequestParam String orderId
    ) {

        return ResponseEntity.ok(
                queueService.startPrinting(orderId)
        );
    }

    @PostMapping("/complete")
    public ResponseEntity<?> complete(
            @RequestParam String orderId
    ) {

        return ResponseEntity.ok(
                queueService.completeOrder(orderId)
        );
    }

    @PostMapping("/proceed")
    public ResponseEntity<?> proceed(
            @RequestParam String orderId
    ) {

        return ResponseEntity.ok(
                queueService.proceedOrder(orderId)
        );
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancel(
            @RequestParam String orderId,
            @RequestParam Long userId
    ) {

        return ResponseEntity.ok(
                queueService.cancelOrder(orderId, userId)
        );
    }

    @GetMapping("/stream/{userId}")
    public SseEmitter streamUpdates(
            @PathVariable Long userId
    ) {
        return sseService.register(userId);
    }

    @PostMapping("/updateProgress")
    public ResponseEntity<?> updateProgress(
            @RequestParam String orderId,
            @RequestParam String progressMessage
    ) {
        return ResponseEntity.ok(
                queueService.updateProgress(orderId, progressMessage)
        );
    }

    @GetMapping("/estimate")
    public ResponseEntity<?> getEstimate(
            @RequestParam String orderId
    ) {
        return ResponseEntity.ok(
                queueService.getQueueEstimate(orderId)
        );
    }
}
