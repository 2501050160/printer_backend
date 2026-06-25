package com.saipraveen.login_registration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.entity.User;
import com.saipraveen.login_registration.service.UserService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private UserService service;

    // Register User
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @RequestBody User user) {

        User savedUser =
                service.registerUser(user);

        return ResponseEntity.ok(savedUser);
    }

    // Login User
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(
            @RequestBody User request) {

        try {
            User user =
                    service.loginUser(
                            request.getUsername(),
                            request.getPassword());

            if (user != null) {
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity
                        .badRequest()
                        .body("Invalid Username or Password");
            }
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}