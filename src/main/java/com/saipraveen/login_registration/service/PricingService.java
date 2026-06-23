package com.saipraveen.login_registration.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.saipraveen.login_registration.entity.Pricing;
import com.saipraveen.login_registration.repository.PricingRepository;

@Service
public class PricingService {

    @Autowired
    private PricingRepository pricingRepository;

    public List<Pricing> getPrices() {

        return pricingRepository.findAll();
    }

    public Pricing updatePrice(
            String printType,
            Double pricePerPage
    ) {

        Pricing pricing =
                pricingRepository.findByPrintType(
                        printType
                );

        if (pricing == null) {

            pricing = new Pricing();
            pricing.setPrintType(
                    printType
            );
        }

        pricing.setPricePerPage(
                pricePerPage
        );

        return pricingRepository.save(
                pricing
        );
    }

    public Double getPrice(
            String printType
    ) {

        Pricing pricing =
                pricingRepository.findByPrintType(
                        printType
                );

        if (pricing == null) {

            return 0.0;
        }

        return pricing.getPricePerPage();
    }
}