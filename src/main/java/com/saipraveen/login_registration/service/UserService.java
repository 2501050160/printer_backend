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

    public User registerUser(User user) {
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            user.setName(user.getUsername());
        }
        if (user.getReferralCode() == null || user.getReferralCode().trim().isEmpty()) {
            String code;
            do {
                code = "REF-" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            } while (repository.findByReferralCode(code) != null);
            user.setReferralCode(code);
        }
        user.setBlocked(false);
        return repository.save(user);
    }

    public User loginUser(String username, String password) {
        User user = repository.findByUsername(username);
        if (user != null) {
            if (Boolean.TRUE.equals(user.getBlocked())) {
                throw new RuntimeException("This account is blocked by Admin.");
            }
            if (user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
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

    @Transactional
    public void deleteUser(Long id) {
        repository.deleteById(id);
    }
}