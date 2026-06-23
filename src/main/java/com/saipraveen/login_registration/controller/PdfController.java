package com.saipraveen.login_registration.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.saipraveen.login_registration.entity.PdfFile;
import com.saipraveen.login_registration.service.PdfFileService;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "http://localhost:5173")
public class PdfController {

@Autowired
private PdfFileService service;

@PostMapping("/updateOrder")
public ResponseEntity<?> updateOrder(

        @RequestParam String orderId,

        @RequestParam Integer copies,

        @RequestParam String selectedPages,
        
        @RequestParam String printType,

        @RequestParam(required = false)
        String blockLocation
) {

    return ResponseEntity.ok(

            service.updateOrder(
                    orderId,
                    copies,
                    selectedPages,
                    printType,
                    blockLocation
            )
    );
}

@PostMapping("/updatePayment")
public ResponseEntity<?> updatePayment(

        @RequestParam Long id,

        @RequestParam String paymentStatus

) {

    return ResponseEntity.ok(

            service.updatePaymentStatus(
                    id,
                    paymentStatus
            )
    );
}


@PostMapping("/upload")
public ResponseEntity<?> uploadPdf(

        @RequestParam("file")
        MultipartFile file,

        @RequestParam("userId")
        Long userId,

        @RequestParam(value = "customerName", required = false)
        String customerName,

        @RequestParam(value = "blockLocation", required = false)
        String blockLocation

) throws IOException {

    return ResponseEntity.ok(

            service.savePdf(
                    file,
                    userId,
                    customerName,
                    blockLocation
            )
    );
}

@GetMapping("/orders")
public ResponseEntity<?> getAllOrders() {

    return ResponseEntity.ok(
            service.getAllOrders()
    );
}
@GetMapping("/userOrders")
public ResponseEntity<?> getUserOrders(

        @RequestParam Long userId

) {

    return ResponseEntity.ok(

            service.getUserOrders(
                    userId
            )
    );
}

@GetMapping("/stats")
public ResponseEntity<?> getStats() {

    return ResponseEntity.ok(
            service.getDashboardStats()
    );
}
@PostMapping("/updateStatus")
public ResponseEntity<?> updateStatus(

        @RequestParam Long id,

        @RequestParam String status

) {

    return ResponseEntity.ok(

            service.updateStatus(
                    id,
                    status
            )
    );
}

@GetMapping("/download/{id}")
public ResponseEntity<byte[]> downloadPdf(
        @PathVariable Long id) {

    PdfFile pdf =
            service.getPdfById(id);

    if (pdf.getPdfData() == null) {

        return ResponseEntity.status(
                HttpStatus.GONE
        ).body(null);
    }

    return ResponseEntity.ok()
            .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\""
                            + pdf.getFileName()
                            + "\""
            )
            .contentType(
                    MediaType.APPLICATION_PDF
            )
            .body(
                    pdf.getPdfData()
            );
}


@PostMapping("/paymentSuccess")
public ResponseEntity<?> paymentSuccess(

        @RequestParam String orderId,

        @RequestParam String paymentId

) {

    return ResponseEntity.ok(

            service.markAsPaid(
                    orderId,
                    paymentId
            )
    );
}

}
