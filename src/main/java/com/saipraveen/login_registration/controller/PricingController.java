package com.saipraveen.login_registration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.service.PricingService;

@RestController
@RequestMapping("/api/pricing")
@CrossOrigin(origins = "http://localhost:5173")
public class PricingController {

    @Autowired
    private PricingService service;

    @GetMapping("/all")
    public ResponseEntity<?> getPrices(@RequestParam(required = false) String blockLocation) {
        if (blockLocation != null && !blockLocation.trim().isEmpty()) {
            return ResponseEntity.ok(service.getPricesByBlock(blockLocation));
        }
        return ResponseEntity.ok(service.getPrices());
    }

    @PostMapping("/update")
    public ResponseEntity<?> updatePrice(
            @RequestParam String printType,
            @RequestParam Double pricePerPage,
            @RequestParam String blockLocation
    ) {
        return ResponseEntity.ok(
                service.updatePrice(printType, pricePerPage, blockLocation)
        );
    }

    @GetMapping("/price")
    public ResponseEntity<?> getPrice(
            @RequestParam String printType,
            @RequestParam String blockLocation
    ) {
        return ResponseEntity.ok(
                service.getPrice(printType, blockLocation)
        );
    }
}