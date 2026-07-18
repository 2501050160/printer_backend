package com.saipraveen.login_registration.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.saipraveen.login_registration.entity.CampusBlock;
import com.saipraveen.login_registration.entity.Pricing;
import com.saipraveen.login_registration.entity.PrinterConfig;
import com.saipraveen.login_registration.entity.SystemSetting;
import com.saipraveen.login_registration.repository.CampusBlockRepository;
import com.saipraveen.login_registration.repository.PricingRepository;
import com.saipraveen.login_registration.repository.PrinterConfigRepository;
import com.saipraveen.login_registration.repository.SystemSettingRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private CampusBlockRepository blockRepository;

    @Autowired
    private PricingRepository pricingRepository;

    @Autowired
    private PrinterConfigRepository printerConfigRepository;

    @Autowired
    private SystemSettingRepository settingRepository;

    @Override
    public void run(String... args) throws Exception {
        // Removed sample initialization of Campus Blocks, Pricing, and Printer Configs
        // as the user manages these dynamically through the Admin Dashboard.

        // 4. Initialize default System Settings if empty
        String[][] settings = {
            {"referralEnabled", "true"},
            {"referrerAmount", "10.0"},
            {"refereeAmount", "5.0"},
            {"popupEnabled", "true"},
            {"popupMessage", "🎉 Refer a friend to earn 10.0 free print credits!"},
            {"adEnabled", "true"},
            {"adText", "Print thesis/assignments directly from your phone skip lines!"},
            {"generalPopupEnabled", "false"},
            {"generalPopupMessage", ""}
        };
        for (String[] pair : settings) {
            if (!settingRepository.existsById(pair[0])) {
                settingRepository.save(new SystemSetting(pair[0], pair[1]));
            }
        }
    }
}
