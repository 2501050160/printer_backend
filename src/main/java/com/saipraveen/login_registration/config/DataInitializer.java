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
        // 1. Initialize Campus Blocks if empty
        if (blockRepository.count() == 0) {
            blockRepository.save(new CampusBlock("C Block"));
            blockRepository.save(new CampusBlock("A Block"));
            blockRepository.save(new CampusBlock("H Block"));
            blockRepository.save(new CampusBlock("IT Block"));
        }

        // 2. Initialize Pricing for each block if empty
        String[] blocks = {"C Block", "A Block", "H Block", "IT Block"};
        for (String block : blocks) {
            if (pricingRepository.findByPrintTypeAndBlockLocation("BW", block) == null) {
                Pricing bwPrice = new Pricing();
                bwPrice.setPrintType("BW");
                bwPrice.setPricePerPage(2.0);
                bwPrice.setBlockLocation(block);
                pricingRepository.save(bwPrice);
            }
            if (pricingRepository.findByPrintTypeAndBlockLocation("COLOR", block) == null) {
                Pricing colorPrice = new Pricing();
                colorPrice.setPrintType("COLOR");
                colorPrice.setPricePerPage(5.0);
                colorPrice.setBlockLocation(block);
                pricingRepository.save(colorPrice);
            }
        }

        // 3. Initialize Printer Configuration for each block if empty
        for (String block : blocks) {
            if (printerConfigRepository.findAll().stream().noneMatch(pc -> block.equals(pc.getBlockLocation()))) {
                PrinterConfig config = new PrinterConfig();
                config.setBlockLocation(block);
                config.setPrinterName(block + " Laser Jet");
                config.setPrinterIp("192.168.1." + (100 + Math.abs(block.hashCode()) % 100));
                config.setActive(true);
                config.setMaintenance(false);
                config.setPaperCount(500);
                config.setQrScanToPrint(true);
                config.setOtpEnabled(true);
                config.setColourSupported(true);
                config.setPaused(false);
                printerConfigRepository.save(config);
            }
        }

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
