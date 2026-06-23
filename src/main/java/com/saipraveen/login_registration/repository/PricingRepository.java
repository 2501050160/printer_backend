package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.saipraveen.login_registration.entity.Pricing;

public interface PricingRepository
        extends JpaRepository<Pricing, Long> {

    Pricing findByPrintType(
            String printType
    );
}