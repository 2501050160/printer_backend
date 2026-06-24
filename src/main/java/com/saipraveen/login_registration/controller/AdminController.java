package com.saipraveen.login_registration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.entity.Admin;
import com.saipraveen.login_registration.service.AdminService;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    @Autowired
    private AdminService service;

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Admin admin
    ) {

        Admin loggedAdmin =
                service.login(
                        admin.getUsername(),
                        admin.getPassword()
                );

        if (loggedAdmin != null) {

            return ResponseEntity.ok(
                    loggedAdmin
            );
        }

        return ResponseEntity
                .badRequest()
                .body("Invalid Admin Credentials");
    }

    @Autowired
    private com.saipraveen.login_registration.service.UserService userService;

    @Autowired
    private com.saipraveen.login_registration.service.PdfFileService pdfFileService;

    @org.springframework.web.bind.annotation.GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/users/toggle-block")
    public ResponseEntity<?> toggleBlockUser(@org.springframework.web.bind.annotation.RequestParam Long id) {
        return ResponseEntity.ok(userService.toggleBlockUser(id));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/users/delete")
    public ResponseEntity<?> deleteUser(@org.springframework.web.bind.annotation.RequestParam Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted");
    }

    @PostMapping("/reset-stats")
    public ResponseEntity<?> resetStats() {
        pdfFileService.resetAllStats();
        return ResponseEntity.ok("Statistics reset successfully");
    }
}