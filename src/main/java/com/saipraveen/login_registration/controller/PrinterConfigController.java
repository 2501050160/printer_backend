package com.saipraveen.login_registration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saipraveen.login_registration.entity.PrinterConfig;
import com.saipraveen.login_registration.service.PrinterConfigService;

@RestController
@RequestMapping("/api/printer")
@CrossOrigin(origins = "http://localhost:5173")
public class PrinterConfigController {

    @Autowired
    private PrinterConfigService service;

@PostMapping("/save")
public ResponseEntity<?> savePrinter(
        @RequestBody PrinterConfig printer
) {

    System.out.println("SAVE API HIT");
    System.out.println("BLOCK = " + printer.getBlockLocation());
    System.out.println("PRINTER = " + printer.getPrinterName());

    return ResponseEntity.ok(
            service.savePrinter(printer)
    );
}
    @GetMapping("/all")
    public ResponseEntity<?> getAllPrinters() {

        return ResponseEntity.ok(
                service.getAllPrinters()
        );
    }

    @GetMapping("/byBlock")
    public ResponseEntity<?> getPrinter(

            @RequestParam String blockLocation

    ) {

        return ResponseEntity.ok(
                service.getPrinterByBlock(
                        blockLocation
                )
        );
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deletePrinter(
            @RequestParam Long id
    ) {

        service.deletePrinter(id);

        return ResponseEntity.ok("Printer deleted");
    }

    @GetMapping("/paper")
    public ResponseEntity<?> getPaperCount(@RequestParam String blockLocation) {
        PrinterConfig printer = service.getPrinterByBlock(blockLocation);
        if (printer == null) {
            return ResponseEntity.ok(0);
        }
        return ResponseEntity.ok(printer.getPaperCount() != null ? printer.getPaperCount() : 0);
    }

    @PostMapping("/updatePaper")
    public ResponseEntity<?> updatePaperCount(
            @RequestParam String blockLocation,
            @RequestParam Integer paperCount
    ) {
        service.updatePaperCount(blockLocation, paperCount);
        return ResponseEntity.ok("Paper count updated successfully");
    }

    @GetMapping("/suggestions")
    public ResponseEntity<?> getSuggestions(
            @RequestParam String currentBlock
    ) {
        return ResponseEntity.ok(
                service.getFallbackSuggestions(currentBlock)
        );
    }
}