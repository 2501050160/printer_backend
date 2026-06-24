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

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private com.saipraveen.login_registration.service.SystemSettingService systemSettingService;

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

    @PostMapping("/sql")
    public ResponseEntity<?> executeSql(@RequestBody java.util.Map<String, String> request) {
        String sql = request.get("query");
        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Query cannot be empty");
        }
        
        String trimmed = sql.trim().toLowerCase();
        try {
            if (trimmed.startsWith("select") || trimmed.startsWith("show") || trimmed.startsWith("describe")) {
                java.util.List<java.util.Map<String, Object>> result = jdbcTemplate.queryForList(sql);
                return ResponseEntity.ok(result);
            } else {
                int rowsAffected = jdbcTemplate.update(sql);
                java.util.Map<String, Object> res = new java.util.HashMap<>();
                res.put("success", true);
                res.put("rowsAffected", rowsAffected);
                res.put("message", "Query executed successfully. Rows affected: " + rowsAffected);
                return ResponseEntity.ok(res);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("SQL Error: " + e.getMessage());
        }
    }

    @org.springframework.web.bind.annotation.GetMapping("/settings")
    public ResponseEntity<?> getSettings() {
        java.util.Map<String, Object> settings = new java.util.HashMap<>();
        settings.put("referralEnabled", systemSettingService.getSettingBool("referral_enabled", true));
        settings.put("referrerAmount", systemSettingService.getSettingDouble("referral_referrer_amount", 10.0));
        settings.put("refereeAmount", systemSettingService.getSettingDouble("referral_referee_amount", 5.0));
        settings.put("popupEnabled", systemSettingService.getSettingBool("referral_popup_enabled", true));
        settings.put("popupMessage", systemSettingService.getSetting("referral_popup_message", ""));
        settings.put("adEnabled", systemSettingService.getSettingBool("ad_enabled", true));
        settings.put("adText", systemSettingService.getSetting("ad_text", ""));
        return ResponseEntity.ok(settings);
    }

    @PostMapping("/settings/update")
    public ResponseEntity<?> updateSettings(@RequestBody java.util.Map<String, Object> request) {
        if (request.containsKey("referralEnabled")) {
            systemSettingService.setSetting("referral_enabled", String.valueOf(request.get("referralEnabled")));
        }
        if (request.containsKey("referrerAmount")) {
            systemSettingService.setSetting("referral_referrer_amount", String.valueOf(request.get("referrerAmount")));
        }
        if (request.containsKey("refereeAmount")) {
            systemSettingService.setSetting("referral_referee_amount", String.valueOf(request.get("refereeAmount")));
        }
        if (request.containsKey("popupEnabled")) {
            systemSettingService.setSetting("referral_popup_enabled", String.valueOf(request.get("popupEnabled")));
        }
        if (request.containsKey("popupMessage")) {
            systemSettingService.setSetting("referral_popup_message", String.valueOf(request.get("popupMessage")));
        }
        if (request.containsKey("adEnabled")) {
            systemSettingService.setSetting("ad_enabled", String.valueOf(request.get("adEnabled")));
        }
        if (request.containsKey("adText")) {
            systemSettingService.setSetting("ad_text", String.valueOf(request.get("adText")));
        }
        return ResponseEntity.ok("Settings updated successfully");
    }
}