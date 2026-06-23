package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.saipraveen.login_registration.entity.PrinterConfig;

public interface PrinterConfigRepository
        extends JpaRepository<PrinterConfig, Long> {

    PrinterConfig findByBlockLocation(
            String blockLocation
    );
}