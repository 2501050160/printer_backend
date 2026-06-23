package com.saipraveen.login_registration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.service.RazorpayService;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    @Autowired
    private RazorpayService service;

    @PostMapping(value = "/createOrder", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createOrder(

            @RequestParam Double amount

    ) throws Exception {

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                service.createOrder(
                        amount
                )
        );
    }
}