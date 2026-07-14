package com.saipraveen.login_registration.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.saipraveen.login_registration.entity.PrinterConfig;
import com.saipraveen.login_registration.repository.PrinterConfigRepository;

@Service
public class PrinterConfigService {

    @Autowired
    private PrinterConfigRepository repository;

    @Autowired
    private SystemSettingService systemSettingService;

    public PrinterConfig savePrinter(
            PrinterConfig printer
    ) {
        PrinterConfig existing = repository.findByBlockLocation(printer.getBlockLocation());
        if (existing != null) {
            existing.setPrinterName(printer.getPrinterName());
            existing.setPrinterIp(printer.getPrinterIp());
            existing.setActive(printer.getActive());
            existing.setMaintenance(printer.getMaintenance());
            existing.setQrScanToPrint(printer.getQrScanToPrint());
            return repository.save(existing);
        }
        return repository.save(
                printer
        );
    }

    public List<PrinterConfig> getAllPrinters() {

        return repository.findAll();
    }

    public PrinterConfig getPrinterByBlock(
            String blockLocation
    ) {

        return repository.findByBlockLocation(
                blockLocation
        );
    }

    public void deletePrinter(Long id) {

        repository.deleteById(id);
    }

    public void decrementPaper(String blockLocation, int pages) {
        PrinterConfig printer = repository.findByBlockLocation(blockLocation);
        if (printer != null) {
            int current = printer.getPaperCount() != null ? printer.getPaperCount() : 0;
            int newCount = Math.max(0, current - pages);
            printer.setPaperCount(newCount);
            repository.save(printer);

            // Check if paper count drops below 50 (warning threshold)
            if (newCount < 50) {
                String adminPhone = "9494189664";
                try {
                    adminPhone = systemSettingService.getSetting("admin_sms_phone", "9494189664");
                } catch (Exception e) {
                    // fallback
                }
                System.out.println("[SMS ALERT to Admin (" + adminPhone + ")]: WARNING! " + blockLocation + 
                    " paper count is running critically low (" + newCount + " pages left). Please refill or restock paper immediately! ⚠️");
            }
        }
    }

    public void updatePaperCount(String blockLocation, int count) {
        PrinterConfig printer = repository.findByBlockLocation(blockLocation);
        if (printer != null) {
            printer.setPaperCount(count);
            repository.save(printer);
        }
    }
}