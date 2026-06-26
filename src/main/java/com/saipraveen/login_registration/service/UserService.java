package com.saipraveen.login_registration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saipraveen.login_registration.entity.User;
import com.saipraveen.login_registration.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private VerificationService verificationService;

    public User registerUser(User user) {
        if (repository.findByEmail(user.getEmail()) != null) {
            throw new RuntimeException("Email is already registered.");
        }
        if (user.getReferralCode() == null || user.getReferralCode().trim().isEmpty()) {
            String code;
            do {
                code = "REF-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            } while (repository.findByReferralCode(code) != null);
            user.setReferralCode(code);
        }
        user.setBlocked(false);
        user.setEmailVerified(false);
        
        // Generate a 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        user.setOtp(otp);
        user.setOtpExpiry(java.time.LocalDateTime.now().plusHours(24));
        
        User savedUser = repository.save(user);
        
        // Also generate verification link token as a fallback
        String linkToken = "";
        try {
            linkToken = verificationService.createVerificationToken(savedUser);
        } catch (Exception ex) {
            System.err.println("Failed to generate verification link token: " + ex.getMessage());
        }
        
        // Construct the fall-back verification URL
        String verifyUrl = "https://printer-backend-34ih.onrender.com/api/verify?token=" + linkToken;
        
        try {
            emailService.sendOtpEmail(
                savedUser.getEmail(),
                otp,
                "Verify your Email - Cloud Print",
                "Welcome to Cloud Print! Please use the following 6-digit OTP code to verify your email address:\n\n%s\n\nAlternatively, if you cannot enter the OTP, click here to verify instantly:\n" + verifyUrl + "\n\nThis code is valid for 24 hours."
            );
        } catch (Exception e) {
            System.err.println("Resend API failed to send email. Storing OTP and link verification token inside database. Console fallback output: " + otp);
        }
        
        return savedUser;
    }

    public User loginUser(String email, String password) {
        User user = repository.findByEmail(email);
        if (user != null) {
            if (Boolean.TRUE.equals(user.getBlocked())) {
                throw new RuntimeException("This account is blocked by Admin.");
            }
            if (user.getEmailVerified() != null && !user.getEmailVerified()) {
                throw new RuntimeException("Email not verified. Please verify your email first.");
            }
            if (user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    @Transactional
    public boolean verifyOtp(String email, String otp) {
        User user = repository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (user.getEmailVerified() != null && user.getEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }
        if (user.getOtp() == null || !user.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired");
        }
        user.setEmailVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        repository.save(user);
        return true;
    }

    @Transactional
    public boolean resendOtp(String email) {
        User user = repository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (user.getEmailVerified() != null && user.getEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        user.setOtp(otp);
        user.setOtpExpiry(java.time.LocalDateTime.now().plusHours(24));
        repository.save(user);
        
        emailService.sendOtpEmail(
            user.getEmail(),
            otp,
            "Cloud Print - Verify your email",
            "Please use the following 6-digit OTP code to verify your email address:\n\n%s\n\nThis code is valid for 24 hours."
        );
        
        return true;
    }

    @Transactional
    public boolean forgotPassword(String email) {
        User user = repository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User with this email does not exist.");
        }
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        user.setOtp(otp);
        user.setOtpExpiry(java.time.LocalDateTime.now().plusMinutes(5));
        repository.save(user);
        
        emailService.sendOtpEmail(
            user.getEmail(),
            otp,
            "Cloud Print - Password Reset Verification Code",
            "You requested a password reset. Your verification code is: %s\nThis code will expire in 5 minutes."
        );
        
        return true;
    }

    @Transactional
    public boolean resetPassword(String email, String otp, String newPassword) {
        User user = repository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (user.getOtp() == null || !user.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired");
        }
        user.setPassword(newPassword);
        user.setOtp(null);
        user.setOtpExpiry(null);
        repository.save(user);
        return true;
    }

    @Transactional
    public void verifyUserEmail(User user) {
        user.setEmailVerified(true);
        repository.save(user);
    }

    public User getUserById(Long userId) {

        return repository.findById(userId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "User not found"
                        )
                );
    }

    public Double getWalletBalance(Long userId) {

        return getUserById(userId).getWalletBalance();
    }

    @Transactional
    public User creditWallet(Long userId, Double amount) {

        User user = getUserById(userId);

        double current =
                user.getWalletBalance() == null
                        ? 0.0
                        : user.getWalletBalance();

        user.setWalletBalance(current + amount);

        return repository.save(user);
    }

    @Transactional
    public User debitWallet(Long userId, Double amount) {

        User user = getUserById(userId);

        double current =
                user.getWalletBalance() == null
                        ? 0.0
                        : user.getWalletBalance();

        if (current < amount) {
            throw new RuntimeException(
                    "Insufficient wallet balance"
            );
        }

        user.setWalletBalance(current - amount);

        return repository.save(user);
    }

    public java.util.List<User> getAllUsers() {
        return repository.findAll();
    }

    @Transactional
    public User toggleBlockUser(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setBlocked(!user.getBlocked());
        return repository.save(user);
    }

    public boolean userExists(Long id) {
        if (id == null) return false;
        return repository.existsById(id);
    }

    @Transactional
    public void deleteUser(Long id) {
        repository.deleteById(id);
    }
}