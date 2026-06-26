package com.saipraveen.login_registration.service;

import java.util.List;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.saipraveen.login_registration.entity.Pricing;
import com.saipraveen.login_registration.repository.PricingRepository;

@Service
public class PricingService {

    @Autowired
    private PricingRepository pricingRepository;

    @Autowired
    private com.saipraveen.login_registration.repository.CampusBlockRepository campusBlockRepository;

    @PostConstruct
    public void initDefaultPrices() {
        String[] blocks = {"C Block", "R Block", "L Block"};
        for (String block : blocks) {
            if (campusBlockRepository.findByName(block) == null) {
                campusBlockRepository.save(new com.saipraveen.login_registration.entity.CampusBlock(block));
            }
            initializeBlockPrice(block, "BW", 2.0);
            initializeBlockPrice(block, "COLOR", 5.0);
        }
    }

    private void initializeBlockPrice(String block, String printType, Double price) {
        Pricing existing = pricingRepository.findByPrintTypeAndBlockLocation(printType, block);
        if (existing == null) {
            Pricing pricing = new Pricing();
            pricing.setBlockLocation(block);
            pricing.setPrintType(printType);
            pricing.setPricePerPage(price);
            pricingRepository.save(pricing);
        }
    }

    public List<Pricing> getPrices() {
        return pricingRepository.findAll();
    }

    public List<Pricing> getPricesByBlock(String blockLocation) {
        return pricingRepository.findByBlockLocation(blockLocation);
    }

    public Pricing updatePrice(String printType, Double pricePerPage, String blockLocation) {
        Pricing pricing = pricingRepository.findByPrintTypeAndBlockLocation(printType, blockLocation);
        if (pricing == null) {
            pricing = new Pricing();
            pricing.setPrintType(printType);
            pricing.setBlockLocation(blockLocation);
        }
        pricing.setPricePerPage(pricePerPage);
        return pricingRepository.save(pricing);
    }

    public Double getPrice(String printType, String blockLocation) {
        Pricing pricing = pricingRepository.findByPrintTypeAndBlockLocation(printType, blockLocation);
        if (pricing == null) {
            // Check global fallback without blockLocation just in case
            java.util.List<Pricing> global = pricingRepository.findByPrintType(printType);
            if (global != null && !global.isEmpty()) {
                return global.get(0).getPricePerPage();
            }
            return 0.0;
        }
        return pricing.getPricePerPage();
    }
}