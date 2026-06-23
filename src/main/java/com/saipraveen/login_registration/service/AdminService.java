package com.saipraveen.login_registration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.saipraveen.login_registration.entity.Admin;
import com.saipraveen.login_registration.repository.AdminRepository;

@Service
public class AdminService {

    @Autowired
    private AdminRepository repository;

    public Admin login(
            String username,
            String password
    ) {

        return repository
                .findByUsernameAndPassword(
                        username,
                        password
                );
    }
}