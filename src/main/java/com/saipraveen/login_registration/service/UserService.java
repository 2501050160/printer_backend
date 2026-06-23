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

        return repository.save(user);
    }

    public User loginUser(String email, String password) {

        User user = repository.findByEmail(email);

        if (user != null &&
                user.getPassword().equals(password)) {

            return user;
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
}