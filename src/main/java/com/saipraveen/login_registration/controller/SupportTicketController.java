package com.saipraveen.login_registration.controller;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.entity.SupportTicket;
import com.saipraveen.login_registration.repository.SupportTicketRepository;

@RestController
@RequestMapping("/api/support")
@CrossOrigin(origins = "http://localhost:5173")
public class SupportTicketController {

    @Autowired
    private SupportTicketRepository repository;

    @PostMapping("/create")
    public ResponseEntity<?> createTicket(@RequestBody SupportTicket ticket) {
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setStatus("PENDING");
        SupportTicket saved = repository.save(ticket);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserTickets(@RequestParam Long userId) {
        return ResponseEntity.ok(repository.findByUserId(userId));
    }

    @PostMapping("/respond")
    public ResponseEntity<?> respondToTicket(
            @RequestParam Long id,
            @RequestParam String response
    ) {
        return repository.findById(id).map(ticket -> {
            ticket.setAdminResponse(response);
            ticket.setUserNotified(false); // user has unseen update
            ticket.setStatus("RESOLVED");
            repository.save(ticket);
            return ResponseEntity.ok(ticket);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/mark-notified")
    public ResponseEntity<?> markNotified(@RequestParam Long userId) {
        java.util.List<SupportTicket> tickets = repository.findByUserId(userId);
        for (SupportTicket t : tickets) {
            if (t.getAdminResponse() != null && !t.isUserNotified()) {
                t.setUserNotified(true);
                repository.save(t);
            }
        }
        return ResponseEntity.ok("Marked all as read");
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllTickets() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping("/resolve")
    public ResponseEntity<?> resolveTicket(@RequestParam Long id) {
        return repository.findById(id).map(ticket -> {
            ticket.setStatus("RESOLVED");
            repository.save(ticket);
            return ResponseEntity.ok("Ticket marked as resolved");
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteTicket(@RequestParam Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.ok("Ticket deleted successfully");
        }
        return ResponseEntity.notFound().build();
    }
}
