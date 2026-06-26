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
import com.saipraveen.login_registration.service.QueueService;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "http://localhost:5173")
public class PdfController {

@Autowired
private PdfFileService service;

@Autowired
private QueueService queueService;

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
        @RequestParam(value = "file", required = false) MultipartFile file,
        @RequestParam(value = "files", required = false) MultipartFile[] files,
        @RequestParam("userId") Long userId,
        @RequestParam(value = "customerName", required = false) String customerName,
        @RequestParam(value = "blockLocation", required = false) String blockLocation
) throws IOException {
    if (files != null && files.length > 0) {
        return ResponseEntity.ok(
                service.saveMultiplePdfs(
                        files,
                        userId,
                        customerName,
                        blockLocation
                )
        );
    } else if (file != null) {
        return ResponseEntity.ok(
                service.savePdf(
                        file,
                        userId,
                        customerName,
                        blockLocation
                )
        );
    } else {
        return ResponseEntity.badRequest().body("No file or files uploaded");
    }
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
public ResponseEntity<?> getStats(
        @RequestParam(defaultValue = "all") String period
) {

    return ResponseEntity.ok(
            service.getDashboardStats(period)
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

    byte[] printableData = service.getPrintablePdfData(pdf);

    if (printableData == null) {

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
                    printableData
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

@PostMapping("/payWithWallet")
public ResponseEntity<?> payWithWallet(

        @RequestParam String orderId

) {

    return ResponseEntity.ok(

            service.payWithWallet(orderId)
    );
}

@PostMapping("/updatePrice")
public ResponseEntity<?> updatePrice(

        @RequestParam String orderId,

        @RequestParam Double price,

        @RequestParam(required = false) Double originalPrice,

        @RequestParam(required = false) Double discountAmount

) {

    return ResponseEntity.ok(

            service.updateFinalPrice(
                    orderId,
                    price,
                    originalPrice,
                    discountAmount
            )
    );
}

@PostMapping("/cancelOrder")
public ResponseEntity<?> cancelOrder(

        @RequestParam String orderId,

        @RequestParam Long userId

) {

    return ResponseEntity.ok(

            queueService.cancelOrder(
                    orderId,
                    userId
            )
    );
}

@GetMapping("/cancelWindow")
public ResponseEntity<?> cancelWindow(

        @RequestParam String orderId

) {

    return ResponseEntity.ok(

            service.getCancelWindowInfo(orderId)
    );
}

    @PostMapping("/applyReferral")
    public ResponseEntity<?> applyReferral(
            @RequestParam String orderId,
            @RequestParam String referralCode,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(
                service.applyReferral(orderId, referralCode, userId)
        );
    }
}
