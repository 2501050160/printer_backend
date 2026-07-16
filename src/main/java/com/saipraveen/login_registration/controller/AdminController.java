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

    @PostMapping("/users/wallet/add")
    public ResponseEntity<?> addWalletBalance(
            @org.springframework.web.bind.annotation.RequestParam Long id,
            @org.springframework.web.bind.annotation.RequestParam Double amount
    ) {
        return ResponseEntity.ok(userService.creditWallet(id, amount));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/users/delete")
    public ResponseEntity<?> deleteUser(@org.springframework.web.bind.annotation.RequestParam Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted");
    }

    @PostMapping("/reset-stats")
    public ResponseEntity<?> resetStats(@org.springframework.web.bind.annotation.RequestParam String adminUsername) {
        if (!"admin".equalsIgnoreCase(adminUsername)) {
            return ResponseEntity.badRequest().body("Only the main admin can reset statistics and database records!");
        }
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
        settings.put("generalPopupEnabled", systemSettingService.getSettingBool("general_popup_enabled", false));
        settings.put("generalPopupMessage", systemSettingService.getSetting("general_popup_message", ""));
        settings.put("thesisDiscountPages", systemSettingService.getSettingDouble("thesis_discount_pages", 50.0));
        settings.put("thesisDiscountPercent", systemSettingService.getSettingDouble("thesis_discount_percent", 15.0));
        settings.put("offpeakDiscountPercent", systemSettingService.getSettingDouble("offpeak_discount_percent", 15.0));
        settings.put("offpeakStartHour", systemSettingService.getSettingDouble("offpeak_start_hour", 21.0));
        settings.put("offpeakEndHour", systemSettingService.getSettingDouble("offpeak_end_hour", 7.0));
        settings.put("offpeakMorningStart", systemSettingService.getSettingDouble("offpeak_morning_start", 7.0));
        settings.put("offpeakMorningEnd", systemSettingService.getSettingDouble("offpeak_morning_end", 9.0));
        settings.put("suspendedColleges", systemSettingService.getSetting("suspended_colleges", ""));
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
        if (request.containsKey("generalPopupEnabled")) {
            systemSettingService.setSetting("general_popup_enabled", String.valueOf(request.get("generalPopupEnabled")));
        }
        if (request.containsKey("generalPopupMessage")) {
            systemSettingService.setSetting("general_popup_message", String.valueOf(request.get("generalPopupMessage")));
        }
        if (request.containsKey("thesisDiscountPages")) {
            systemSettingService.setSetting("thesis_discount_pages", String.valueOf(request.get("thesisDiscountPages")));
        }
        if (request.containsKey("thesisDiscountPercent")) {
            systemSettingService.setSetting("thesis_discount_percent", String.valueOf(request.get("thesisDiscountPercent")));
        }
        if (request.containsKey("offpeakDiscountPercent")) {
            systemSettingService.setSetting("offpeak_discount_percent", String.valueOf(request.get("offpeakDiscountPercent")));
        }
        if (request.containsKey("offpeakStartHour")) {
            systemSettingService.setSetting("offpeak_start_hour", String.valueOf(request.get("offpeakStartHour")));
        }
        if (request.containsKey("offpeakEndHour")) {
            systemSettingService.setSetting("offpeak_end_hour", String.valueOf(request.get("offpeakEndHour")));
        }
        if (request.containsKey("offpeakMorningStart")) {
            systemSettingService.setSetting("offpeak_morning_start", String.valueOf(request.get("offpeakMorningStart")));
        }
        if (request.containsKey("offpeakMorningEnd")) {
            systemSettingService.setSetting("offpeak_morning_end", String.valueOf(request.get("offpeakMorningEnd")));
        }
        if (request.containsKey("suspendedColleges")) {
            systemSettingService.setSetting("suspended_colleges", String.valueOf(request.get("suspendedColleges")));
        }
        return ResponseEntity.ok("Settings updated successfully");
    }

    @org.springframework.web.bind.annotation.GetMapping("/settings/offpeak")
    public ResponseEntity<?> getCollegeOffpeakSettings(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "KLU") String college) {
        java.util.Map<String, Object> settings = new java.util.HashMap<>();
        settings.put("offpeakEnabled", systemSettingService.getSettingBool("offpeak_enabled_" + college, systemSettingService.getSettingBool("offpeak_enabled", true)));
        settings.put("offpeakDiscountPercent", systemSettingService.getSettingDouble("offpeak_discount_percent_" + college, systemSettingService.getSettingDouble("offpeak_discount_percent", 15.0)));
        settings.put("offpeakStartHour", systemSettingService.getSettingDouble("offpeak_start_hour_" + college, systemSettingService.getSettingDouble("offpeak_start_hour", 21.0)));
        settings.put("offpeakEndHour", systemSettingService.getSettingDouble("offpeak_end_hour_" + college, systemSettingService.getSettingDouble("offpeak_end_hour", 7.0)));
        settings.put("offpeakMorningStart", systemSettingService.getSettingDouble("offpeak_morning_start_" + college, systemSettingService.getSettingDouble("offpeak_morning_start", 7.0)));
        settings.put("offpeakMorningEnd", systemSettingService.getSettingDouble("offpeak_morning_end_" + college, systemSettingService.getSettingDouble("offpeak_morning_end", 9.0)));
        return ResponseEntity.ok(settings);
    }

    @PostMapping("/settings/offpeak/update")
    public ResponseEntity<?> updateCollegeOffpeakSettings(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "KLU") String college,
            @RequestBody java.util.Map<String, Object> request) {
        if (request.containsKey("offpeakEnabled")) {
            systemSettingService.setSetting("offpeak_enabled_" + college, String.valueOf(request.get("offpeakEnabled")));
        }
        if (request.containsKey("offpeakDiscountPercent")) {
            systemSettingService.setSetting("offpeak_discount_percent_" + college, String.valueOf(request.get("offpeakDiscountPercent")));
        }
        if (request.containsKey("offpeakStartHour")) {
            systemSettingService.setSetting("offpeak_start_hour_" + college, String.valueOf(request.get("offpeakStartHour")));
        }
        if (request.containsKey("offpeakEndHour")) {
            systemSettingService.setSetting("offpeak_end_hour_" + college, String.valueOf(request.get("offpeakEndHour")));
        }
        if (request.containsKey("offpeakMorningStart")) {
            systemSettingService.setSetting("offpeak_morning_start_" + college, String.valueOf(request.get("offpeakMorningStart")));
        }
        if (request.containsKey("offpeakMorningEnd")) {
            systemSettingService.setSetting("offpeak_morning_end_" + college, String.valueOf(request.get("offpeakMorningEnd")));
        }
        return ResponseEntity.ok("College off-peak settings updated successfully");
    }

    @org.springframework.web.bind.annotation.GetMapping("/printers/status")
    public ResponseEntity<?> getPrintersStatus() {
        return ResponseEntity.ok(pdfFileService.getPrinterLiveStatusList());
    }

    @org.springframework.web.bind.annotation.GetMapping("/subadmins")
    public ResponseEntity<?> getAllSubAdmins() {
        return ResponseEntity.ok(service.getAllSubAdmins());
    }

    @PostMapping("/subadmins/create")
    public ResponseEntity<?> createSubAdmin(@RequestBody Admin subAdmin) {
        try {
            return ResponseEntity.ok(service.createSubAdmin(subAdmin));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/subadmins/delete")
    public ResponseEntity<?> deleteSubAdmin(@org.springframework.web.bind.annotation.RequestParam Long id) {
        service.deleteSubAdmin(id);
        return ResponseEntity.ok("Sub-admin deleted successfully");
    }
}