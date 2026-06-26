package com.saipraveen.login_registration.controller;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/all")
    public ResponseEntity<?> getAllTickets() {
        return ResponseEntity.ok(repository.findAll());
    }
}
