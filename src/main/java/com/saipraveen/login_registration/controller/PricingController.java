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
    public ResponseEntity<?> getPrices() {

        return ResponseEntity.ok(
                service.getPrices()
        );
    }

    @PostMapping("/update")
    public ResponseEntity<?> updatePrice(

            @RequestParam String printType,

            @RequestParam Double pricePerPage

    ) {

        return ResponseEntity.ok(

                service.updatePrice(
                        printType,
                        pricePerPage
                )
        );
    }

    @GetMapping("/price")
    public ResponseEntity<?> getPrice(

            @RequestParam String printType

    ) {

        return ResponseEntity.ok(

                service.getPrice(
                        printType
                )
        );
    }
}