package com.saipraveen.login_registration.controller;

import com.saipraveen.login_registration.service.PdfFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import org.json.JSONObject;

@RestController
@RequestMapping("/api/webhook")
@CrossOrigin(origins = "*") // Allow Razorpay to POST from any origin
public class RazorpayWebhookController {

    @Autowired
    private PdfFileService pdfService;

    // Razorpay secret from application.properties
    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Razorpay webhook endpoint.
     * Verifies the signature and updates payment status based on the event.
     */
    @PostMapping("/razorpay")
    public ResponseEntity<String> handle(@RequestHeader("X-Razorpay-Signature") String signature,
                                         @RequestBody String payload) {
        try {
            // Verify the incoming signature
            if (!verifySignature(payload, signature)) {
                return new ResponseEntity<>("Invalid signature", HttpStatus.BAD_REQUEST);
            }

            JSONObject event = new JSONObject(payload);
            String eventName = event.getString("event");
            JSONObject payment = event.getJSONObject("payload")
                                      .getJSONObject("payment")
                                      .getJSONObject("entity");
            
            // Retrieve application order ID from payment notes, falling back to Razorpay order_id
            String orderId = null;
            if (payment.has("notes") && !payment.isNull("notes")) {
                JSONObject notes = payment.getJSONObject("notes");
                if (notes.has("app_order_id")) {
                    orderId = notes.getString("app_order_id");
                }
            }
            if (orderId == null || orderId.trim().isEmpty()) {
                orderId = payment.getString("order_id");
            }
            
            String status = payment.getString("status"); // e.g., "captured", "failed"

            // Update our system using order ID
            pdfService.updatePaymentStatusByOrderId(orderId, status.toUpperCase());

            return new ResponseEntity<>("Webhook processed", HttpStatus.OK);
        } catch (Exception e) {
            // In production, log the error details
            return new ResponseEntity<>("Error processing webhook", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Verify Razorpay signature using HMAC SHA256 and Base64 encoding.
     */
    private boolean verifySignature(String payload, String receivedSignature) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(razorpayKeySecret.getBytes(), HMAC_SHA256);
            mac.init(secretKey);
            byte[] digest = mac.doFinal(payload.getBytes());
            String generated = Base64.getEncoder().encodeToString(digest);
            return generated.equals(receivedSignature);
        } catch (Exception e) {
            return false;
        }
    }
}
