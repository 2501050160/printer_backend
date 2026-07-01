package com.saipraveen.login_registration.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.saipraveen.login_registration.entity.Reward;
import com.saipraveen.login_registration.entity.UserRewardClaim;
import com.saipraveen.login_registration.repository.RewardRepository;
import com.saipraveen.login_registration.repository.UserRewardClaimRepository;
import com.saipraveen.login_registration.service.UserService;

@RestController
@RequestMapping("/api/rewards")
@CrossOrigin(origins = "http://localhost:5173")
public class RewardController {

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private UserRewardClaimRepository claimRepository;

    @Autowired
    private UserService userService;

    @GetMapping("/all")
    public ResponseEntity<List<Reward>> getAllRewards() {
        return ResponseEntity.ok(rewardRepository.findAll());
    }

    @PostMapping("/create")
    public ResponseEntity<?> createReward(@RequestBody Reward reward) {
        if (reward.getClaimCode() == null || reward.getClaimCode().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Claim code cannot be empty");
        }
        if (reward.getRewardAmount() == null || reward.getRewardAmount() <= 0) {
            return ResponseEntity.badRequest().body("Reward amount must be greater than 0");
        }
        
        reward.setClaimCode(reward.getClaimCode().trim().toUpperCase());
        Reward existing = rewardRepository.findByClaimCode(reward.getClaimCode());
        if (existing != null) {
            return ResponseEntity.badRequest().body("Reward with this claim code already exists");
        }
        
        if (reward.getClaimedCount() == null) {
            reward.setClaimedCount(0);
        }
        if (reward.getActive() == null) {
            reward.setActive(true);
        }
        
        Reward saved = rewardRepository.save(reward);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/claim")
    public ResponseEntity<?> claimReward(@RequestParam Long userId, @RequestParam String claimCode) {
        Map<String, Object> response = new HashMap<>();
        if (claimCode == null || claimCode.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Claim code cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }

        String normalizedCode = claimCode.trim().toUpperCase();
        
        // Also support claiming referrals directly via the same box if it is entered here!
        // We can check if it matches a user's referral code and handle it, or stick strictly to reward claim code.
        // Wait, standard reward code claim:
        Reward reward = rewardRepository.findByClaimCode(normalizedCode);
        if (reward == null) {
            response.put("success", false);
            response.put("message", "Invalid reward claim code");
            return ResponseEntity.badRequest().body(response);
        }

        if (!reward.getActive()) {
            response.put("success", false);
            response.put("message", "This reward is currently inactive");
            return ResponseEntity.badRequest().body(response);
        }

        if (reward.getExpiryDate() != null && java.time.LocalDate.now().isAfter(reward.getExpiryDate())) {
            response.put("success", false);
            response.put("message", "This reward voucher has expired");
            return ResponseEntity.badRequest().body(response);
        }

        if (reward.getClaimedCount() >= reward.getMaxClaims()) {
            response.put("success", false);
            response.put("message", "This reward claim limit has been reached");
            return ResponseEntity.badRequest().body(response);
        }

        boolean alreadyClaimed = claimRepository.existsByUserIdAndRewardId(userId, reward.getId());
        if (alreadyClaimed) {
            response.put("success", false);
            response.put("message", "You have already claimed this reward");
            return ResponseEntity.badRequest().body(response);
        }

        // Process claim (credit wallet balance)
        userService.creditWallet(userId, reward.getRewardAmount(), "REWARD", "Redeemed claim code: " + reward.getClaimCode());
        
        // Record claim
        claimRepository.save(new UserRewardClaim(userId, reward.getId(), LocalDateTime.now()));
        
        // Increment claimed count
        reward.setClaimedCount(reward.getClaimedCount() + 1);
        rewardRepository.save(reward);

        response.put("success", true);
        response.put("message", "Reward claimed successfully! Rs. " + reward.getRewardAmount() + " added to your wallet.");
        response.put("amount", reward.getRewardAmount());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-status")
    public ResponseEntity<?> updateStatus(@RequestParam Long id, @RequestParam Boolean active) {
        return rewardRepository.findById(id).map(rew -> {
            rew.setActive(active);
            rewardRepository.save(rew);
            return ResponseEntity.ok("Reward status updated");
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteReward(@RequestParam Long id) {
        if (rewardRepository.existsById(id)) {
            rewardRepository.deleteById(id);
            return ResponseEntity.ok("Reward deleted successfully");
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/generate-batch")
    public ResponseEntity<?> generateVoucherBatch(
            @RequestParam String prefix,
            @RequestParam Double amount,
            @RequestParam Integer count,
            @RequestParam Integer maxClaims,
            @RequestParam(required = false) String expiryDate
    ) {
        java.time.LocalDate localExpiryDate = null;
        if (expiryDate != null && !expiryDate.trim().isEmpty()) {
            try {
                localExpiryDate = java.time.LocalDate.parse(expiryDate.trim());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Invalid expiry date format. Expected YYYY-MM-DD.");
            }
        }

        java.util.List<Reward> createdVouchers = new java.util.ArrayList<>();
        java.util.Random random = new java.util.Random();
        String upperPrefix = prefix.trim().toUpperCase();

        for (int i = 0; i < count; i++) {
            String claimCode;
            Reward existing;
            do {
                StringBuilder randomPart = new StringBuilder();
                String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
                for (int k = 0; k < 5; k++) {
                    randomPart.append(chars.charAt(random.nextInt(chars.length())));
                }
                claimCode = upperPrefix + "-" + randomPart.toString();
                existing = rewardRepository.findByClaimCode(claimCode);
            } while (existing != null);

            Reward reward = new Reward();
            reward.setTitle("Voucher " + claimCode);
            reward.setDescription("Reward voucher worth Rs. " + amount);
            reward.setRewardAmount(amount);
            reward.setClaimCode(claimCode);
            reward.setMaxClaims(maxClaims);
            reward.setClaimedCount(0);
            reward.setActive(true);
            reward.setExpiryDate(localExpiryDate);

            createdVouchers.add(rewardRepository.save(reward));
        }

        return ResponseEntity.ok(createdVouchers);
    }
}
