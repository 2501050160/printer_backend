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

    @PostMapping("/users/reward")
    public ResponseEntity<?> rewardUser(
            @org.springframework.web.bind.annotation.RequestParam Long id,
            @org.springframework.web.bind.annotation.RequestParam Double amount,
            @org.springframework.web.bind.annotation.RequestParam String reason
    ) {
        try {
            String desc = (reason == null || reason.trim().isEmpty()) ? "Gifted by Admin" : reason.trim();
            com.saipraveen.login_registration.entity.User user = userService.creditWallet(id, amount, "REWARD", desc);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
        settings.put("generalPopupEnabled", systemSettingService.getSettingBool("general_popup_enabled", false));
        settings.put("generalPopupMessage", systemSettingService.getSetting("general_popup_message", ""));
        settings.put("scratchCardMinReward", systemSettingService.getSettingDouble("scratch_card_min_reward", 1.0));
        settings.put("scratchCardMaxReward", systemSettingService.getSettingDouble("scratch_card_max_reward", 10.0));
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
        if (request.containsKey("scratchCardMinReward")) {
            systemSettingService.setSetting("scratch_card_min_reward", String.valueOf(request.get("scratchCardMinReward")));
        }
        if (request.containsKey("scratchCardMaxReward")) {
            systemSettingService.setSetting("scratch_card_max_reward", String.valueOf(request.get("scratchCardMaxReward")));
        }
        return ResponseEntity.ok("Settings updated successfully");
    }

    @org.springframework.web.bind.annotation.GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics() {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        
        // 1. Daily print volume trends (for last 7 days)
        String dailyVolumeSql = "SELECT CAST(upload_time AS date) as date, COUNT(*) as count, COALESCE(SUM(total_pages),0) as pages " +
                "FROM pdf_files WHERE status='COMPLETED' AND upload_time >= CURRENT_DATE - INTERVAL '7 days' " +
                "GROUP BY CAST(upload_time AS date) ORDER BY date";
        try {
            data.put("dailyVolume", jdbcTemplate.queryForList(dailyVolumeSql));
        } catch (Exception e) {
            data.put("dailyVolume", java.util.Collections.emptyList());
        }

        // 2. Revenue breakdowns by campus block
        String blockRevenueSql = "SELECT block_location as block, COALESCE(SUM(price), 0) as revenue, COUNT(*) as count " +
                "FROM pdf_files WHERE payment_status='PAID' AND status != 'CANCELLED' GROUP BY block_location";
        try {
            data.put("blockRevenue", jdbcTemplate.queryForList(blockRevenueSql));
        } catch (Exception e) {
            data.put("blockRevenue", java.util.Collections.emptyList());
        }

        // 3. Color vs BW print ratio
        String printTypeRatioSql = "SELECT COALESCE(print_type, 'BW') as print_type, COUNT(*) as count, COALESCE(SUM(total_pages), 0) as pages " +
                "FROM pdf_files WHERE payment_status='PAID' AND status != 'CANCELLED' GROUP BY print_type";
        try {
            data.put("printTypeRatio", jdbcTemplate.queryForList(printTypeRatioSql));
        } catch (Exception e) {
            data.put("printTypeRatio", java.util.Collections.emptyList());
        }

        return ResponseEntity.ok(data);
    }
}