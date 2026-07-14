package com.saipraveen.login_registration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.entity.PdfFile;
import com.saipraveen.login_registration.service.QueueService;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    @Autowired
    private QueueService queueService;

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
        try {
            PdfFile next =
                    queueService.getNextForAgent(blockLocation);

            if (next == null) {
                return ResponseEntity.ok(null);
            }

            return ResponseEntity.ok(next);
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return ResponseEntity.status(500).body("Error: " + e.getMessage() + "\nStacktrace:\n" + sw.toString());
        }
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

    @PostMapping("/progress")
    public ResponseEntity<?> updateProgress(
            @RequestParam String orderId,
            @RequestParam Integer printedPages
    ) {
        return ResponseEntity.ok(
                queueService.updateProgress(orderId, printedPages)
        );
    }
}
