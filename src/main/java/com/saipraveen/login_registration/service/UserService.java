package com.saipraveen.login_registration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}