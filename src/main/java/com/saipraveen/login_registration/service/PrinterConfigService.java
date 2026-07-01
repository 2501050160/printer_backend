package com.saipraveen.login_registration.service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.saipraveen.login_registration.entity.PrinterConfig;
import com.saipraveen.login_registration.entity.PdfFile;
import com.saipraveen.login_registration.repository.PrinterConfigRepository;
import com.saipraveen.login_registration.repository.PdfFileRepository;

@Service
public class PrinterConfigService {

    @Autowired
    private PrinterConfigRepository repository;

    @Autowired
    private PdfFileRepository pdfFileRepository;

    @Autowired
    private EmailService emailService;

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

            // Check if paper level drops below critical threshold (< 20 sheets)
            if (newCount < 20 && current >= 20) {
                try {
                    emailService.sendEmail(
                        "saipraveendasari2@gmail.com",
                        "CRITICAL ALERT: Low Supplies at " + blockLocation,
                        "Technician Alert:\n\nThe printer located at " + blockLocation + 
                        " is running extremely low on paper.\nRemaining paper count: " + newCount + 
                        " sheets.\n\nPlease refill the paper tray immediately to prevent printing downtime."
                    );
                } catch (Exception e) {
                    System.err.println("Failed to send low paper email alert: " + e.getMessage());
                }
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

    public List<Map<String, Object>> getFallbackSuggestions(String currentBlock) {
        List<PrinterConfig> printers = repository.findAll();
        List<Map<String, Object>> suggestions = new ArrayList<>();
        
        for (PrinterConfig printer : printers) {
            if (printer.getBlockLocation().equalsIgnoreCase(currentBlock)) {
                continue;
            }
            if (Boolean.TRUE.equals(printer.getActive()) && !Boolean.TRUE.equals(printer.getMaintenance()) && printer.getPaperCount() != null && printer.getPaperCount() > 10) {
                Map<String, Object> sug = new HashMap<>();
                sug.put("blockLocation", printer.getBlockLocation());
                sug.put("printerName", printer.getPrinterName());
                sug.put("paperCount", printer.getPaperCount());
                
                List<PdfFile> activeQueue = pdfFileRepository.findActiveQueueByBlock(printer.getBlockLocation());
                int totalPages = 0;
                for (PdfFile pdf : activeQueue) {
                    totalPages += (pdf.getTotalPages() != null ? pdf.getTotalPages() : 1) * (pdf.getCopies() != null ? pdf.getCopies() : 1);
                }
                double waitTimeSec = (totalPages * 3.0) + (activeQueue.size() * 10.0);
                sug.put("estimatedWaitTimeMinutes", Math.ceil(waitTimeSec / 60.0 * 10.0) / 10.0);
                sug.put("queueLength", activeQueue.size());
                
                suggestions.add(sug);
            }
        }
        
        suggestions.sort((a, b) -> Double.compare((Double)a.get("estimatedWaitTimeMinutes"), (Double)b.get("estimatedWaitTimeMinutes")));
        return suggestions;
    }
}