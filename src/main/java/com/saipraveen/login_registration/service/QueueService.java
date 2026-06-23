package com.saipraveen.login_registration.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saipraveen.login_registration.entity.PdfFile;
import com.saipraveen.login_registration.repository.PdfFileRepository;

@Service
public class QueueService {

    @Autowired
    private PdfFileRepository repository;

    @Autowired
    private UserService userService;

    @Value("${print.cancel-window-seconds:30}")
    private int cancelWindowSeconds;

    @Value("${print.fulfillment-timeout-minutes:5}")
    private int fulfillmentTimeoutMinutes;

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void promoteExpiredCancelWindows() {

        List<PdfFile> expired =
                repository.findExpiredCancelWindows(
                        LocalDateTime.now()
                );

        for (PdfFile pdf : expired) {

            pdf.setStatus("QUEUE");
            pdf.setQueuedAt(LocalDateTime.now());

            repository.save(pdf);

            System.out.println(
                    "Order promoted to queue: "
                            + pdf.getOrderId()
            );
        }
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void cancelTimedOutOrders() {

        LocalDateTime cutoff =
                LocalDateTime.now()
                        .minusMinutes(
                                fulfillmentTimeoutMinutes
                        );

        List<PdfFile> timedOut =
                repository.findTimedOutOrders(cutoff);

        for (PdfFile pdf : timedOut) {

            refundAndCancel(
                    pdf,
                    "Print not completed within "
                            + fulfillmentTimeoutMinutes
                            + " minutes"
            );

            System.out.println(
                    "Order timed out and refunded: "
                            + pdf.getOrderId()
                            + " (Print not completed within "
                            + fulfillmentTimeoutMinutes
                            + " minutes)"
            );
        }
    }

    public List<PdfFile> getQueueByBlock(String blockLocation) {

        return repository.findQueueByBlock(
                normalizeBlock(blockLocation)
        );
    }

    public List<PdfFile> getActiveQueueByBlock(String blockLocation) {

        return repository.findActiveQueueByBlock(
                normalizeBlock(blockLocation)
        );
    }

    public PdfFile getNextForAgent(String blockLocation) {

        List<PdfFile> queue =
                repository.findQueueByBlock(
                        normalizeBlock(blockLocation)
                );

        if (queue.isEmpty()) {
            return null;
        }

        return queue.get(0);
    }

    public PdfFile startPrinting(String orderId) {

        PdfFile pdf =
                repository.findByOrderId(orderId);

        if (pdf == null) {
            throw new RuntimeException("Order not found");
        }

        if (!"QUEUE".equals(pdf.getStatus())) {
            throw new RuntimeException(
                    "Order is not ready for printing"
            );
        }

        pdf.setStatus("PRINTING");
        pdf.setPrintingStartedAt(LocalDateTime.now());

        return repository.save(pdf);
    }

    public PdfFile completeOrder(String orderId) {

        PdfFile pdf =
                repository.findByOrderId(orderId);

        if (pdf == null) {
            throw new RuntimeException("Order not found");
        }

        pdf.setStatus("COMPLETED");
        pdf.setFinishedAt(LocalDateTime.now());

        return repository.save(pdf);
    }

    @Transactional
    public Map<String, Object> cancelOrder(
            String orderId,
            Long userId
    ) {

        PdfFile pdf =
                repository.findByOrderId(orderId);

        Map<String, Object> result =
                new HashMap<>();

        if (pdf == null) {
            result.put("success", false);
            result.put("message", "Order not found");
            return result;
        }

        if (!"CANCEL_WINDOW".equals(pdf.getStatus())) {
            result.put("success", false);
            result.put("message", "Cancel window has expired");
            return result;
        }

        if (pdf.getCancelWindowEndsAt() != null
                && LocalDateTime.now().isAfter(
                        pdf.getCancelWindowEndsAt()
                )) {

            result.put("success", false);
            result.put("message", "Cancel window has expired");
            return result;
        }

        if (userId != null
                && pdf.getUserId() != null
                && !userId.equals(pdf.getUserId())) {

            result.put("success", false);
            result.put("message", "Unauthorized");
            return result;
        }

        Double refundAmount =
                pdf.getPrice() == null
                        ? 0.0
                        : pdf.getPrice();

        if (pdf.getUserId() != null && refundAmount > 0) {
            userService.creditWallet(
                    pdf.getUserId(),
                    refundAmount
            );
        }

        pdf.setStatus("CANCELLED");
        pdf.setPaymentStatus("REFUNDED");
        pdf.setFinishedAt(LocalDateTime.now());
        pdf.setPdfData(null);

        repository.save(pdf);

        result.put("success", true);
        result.put("message", "Order cancelled. Amount credited to wallet.");
        result.put("refundAmount", refundAmount);

        return result;
    }

    public void beginCancelWindow(PdfFile pdf) {

        LocalDateTime now = LocalDateTime.now();

        pdf.setPaidAt(now);
        pdf.setCancelWindowEndsAt(
                now.plusSeconds(cancelWindowSeconds)
        );
        pdf.setPaymentStatus("PAID");
        pdf.setStatus("CANCEL_WINDOW");

        if (pdf.getOriginalPrice() == null && pdf.getPrice() != null) {
            pdf.setOriginalPrice(pdf.getPrice());
        }
    }

    private void refundAndCancel(PdfFile pdf, String reason) {

        Double refundAmount =
                pdf.getPrice() == null
                        ? 0.0
                        : pdf.getPrice();

        if (pdf.getUserId() != null && refundAmount > 0) {
            userService.creditWallet(
                    pdf.getUserId(),
                    refundAmount
            );
        }

        pdf.setStatus("CANCELLED");
        pdf.setPaymentStatus("REFUNDED");
        pdf.setFinishedAt(LocalDateTime.now());
        pdf.setPdfData(null);

        repository.save(pdf);
    }

    public int getCancelWindowSeconds() {
        return cancelWindowSeconds;
    }

    private String normalizeBlock(String blockLocation) {

        if (blockLocation == null
                || blockLocation.trim().isEmpty()) {

            return "C Block";
        }

        return blockLocation.trim();
    }
}
