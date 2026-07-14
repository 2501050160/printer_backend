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


    public PrinterConfig savePrinter(
            PrinterConfig printer
    ) {
        PrinterConfig existing = null;
        if (printer.getId() != null) {
            existing = repository.findById(printer.getId()).orElse(null);
        }
        if (existing == null) {
            existing = repository.findByBlockLocation(printer.getBlockLocation());
        }

        if (existing != null) {
            existing.setBlockLocation(printer.getBlockLocation());
            existing.setPrinterName(printer.getPrinterName());
            existing.setPrinterIp(printer.getPrinterIp());
            existing.setActive(printer.getActive());
            existing.setMaintenance(printer.getMaintenance());
            existing.setQrScanToPrint(printer.getQrScanToPrint());
            existing.setOtpEnabled(printer.getOtpEnabled());
            existing.setColourSupported(printer.getColourSupported());
            existing.setPaused(printer.getPaused());
            if (printer.getPaperCount() != null) {
                existing.setPaperCount(printer.getPaperCount());
            }
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