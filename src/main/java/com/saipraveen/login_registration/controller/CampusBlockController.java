package com.saipraveen.login_registration.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.saipraveen.login_registration.entity.CampusBlock;
import com.saipraveen.login_registration.repository.CampusBlockRepository;
import com.saipraveen.login_registration.service.PricingService;

@RestController
@RequestMapping("/api/blocks")
@CrossOrigin(origins = "http://localhost:5173")
public class CampusBlockController {

    @Autowired
    private CampusBlockRepository repository;

    @Autowired
    private PricingService pricingService;

    @GetMapping("/all")
    public ResponseEntity<List<CampusBlock>> getAllBlocks() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping("/add")
    public ResponseEntity<?> addBlock(@RequestParam String name) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Block name cannot be empty");
        }
        
        String trimmed = name.trim();
        CampusBlock existing = repository.findByName(trimmed);
        if (existing != null) {
            return ResponseEntity.badRequest().body("Block already exists");
        }

        CampusBlock block = repository.save(new CampusBlock(trimmed));
        
        // Auto-initialize prices for the new block so that we can price orders
        pricingService.updatePrice("BW", 2.0, trimmed);
        pricingService.updatePrice("COLOR", 5.0, trimmed);
        
        return ResponseEntity.ok(block);
    }
}
