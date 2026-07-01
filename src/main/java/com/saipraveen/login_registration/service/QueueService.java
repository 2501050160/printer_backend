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

    @Autowired
    private PrinterConfigService printerConfigService;

    @Autowired
    private SseService sseService;

    @Autowired
    private EmailService emailService;

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
            pdf.setStatus("PENDING_SCAN");
            pdf.setPrintProgress("Waiting for QR Scan verification");
            repository.save(pdf);
            sseService.sendProgress(pdf.getUserId(), pdf.getOrderId(), "PENDING_SCAN", "Waiting for QR Scan verification");
            sseService.sendQueueUpdate(pdf.getUserId());
            System.out.println("Order held in PENDING_SCAN for OTP verification: " + pdf.getOrderId());
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

        // 10-Minute PENDING_SCAN verification timeout
        LocalDateTime scanCutoff = LocalDateTime.now().minusMinutes(10);
        List<PdfFile> scanTimedOut = repository.findExpiredPendingScanOrders(scanCutoff);
        for (PdfFile pdf : scanTimedOut) {
            refundAndCancel(
                    pdf,
                    "QR/OTP Scan verification timeout (10 minutes)"
            );

            System.out.println(
                    "Order scan verification timed out, refunded and deleted file data: "
                            + pdf.getOrderId()
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
        recordHeartbeat(blockLocation);

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
        pdf.setPrintProgress("Converting to print layout...");
        PdfFile saved = repository.save(pdf);
        sseService.sendProgress(pdf.getUserId(), pdf.getOrderId(), "PRINTING", "Converting to print layout...");
        sseService.sendQueueUpdate(pdf.getUserId());
        return saved;
    }

    @Transactional
    public PdfFile proceedOrder(String orderId) {
        PdfFile pdf = repository.findByOrderId(orderId);
        if (pdf == null) {
            throw new RuntimeException("Order not found");
        }
        if ("CANCEL_WINDOW".equals(pdf.getStatus())) {
            pdf.setStatus("PENDING_SCAN");
            return repository.save(pdf);
        }
        return pdf;
    }

    public PdfFile completeOrder(String orderId) {

        PdfFile pdf =
                repository.findByOrderId(orderId);

        if (pdf == null) {
            throw new RuntimeException("Order not found");
        }

        pdf.setStatus("COMPLETED");
        pdf.setFinishedAt(LocalDateTime.now());
        pdf.setPdfData(null); // Delete the PDF binary file data immediately after printing is completed
        pdf.setPrintProgress("Ready for pickup! 📄");

        try {
            int pages = (pdf.getTotalPages() != null ? pdf.getTotalPages() : 1) * (pdf.getCopies() != null ? pdf.getCopies() : 1);
            printerConfigService.decrementPaper(pdf.getBlockLocation(), pages);
        } catch (Exception e) {
            System.err.println("Failed to decrement paper count for block: " + pdf.getBlockLocation() + " - " + e.getMessage());
        }

        PdfFile saved = repository.save(pdf);
        sseService.sendProgress(pdf.getUserId(), pdf.getOrderId(), "COMPLETED", "Ready for pickup! 📄");
        sseService.sendQueueUpdate(pdf.getUserId());
        return saved;
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

        boolean isCancelable = "CANCEL_WINDOW".equals(pdf.getStatus()) || "PENDING_SCAN".equals(pdf.getStatus());

        if (!isCancelable) {
            result.put("success", false);
            result.put("message", "Order cannot be cancelled at this stage");
            return result;
        }

        if ("CANCEL_WINDOW".equals(pdf.getStatus())) {
            if (pdf.getCancelWindowEndsAt() != null
                    && LocalDateTime.now().isAfter(
                            pdf.getCancelWindowEndsAt()
                    )) {
                result.put("success", false);
                result.put("message", "Cancel window has expired");
                return result;
            }
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
                    refundAmount,
                    "REFUND",
                    "Refund for cancelled order: " + pdf.getOrderId()
            );
        }

        pdf.setStatus("CANCELLED");
        pdf.setPaymentStatus("REFUNDED");
        pdf.setFinishedAt(LocalDateTime.now());
        pdf.setPdfData(null);
        pdf.setPrintProgress("Cancelled");

        repository.save(pdf);
        sseService.sendProgress(pdf.getUserId(), pdf.getOrderId(), "CANCELLED", "Cancelled");
        sseService.sendQueueUpdate(pdf.getUserId());

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
        pdf.setPrintProgress("Cancel window active");

        // Generate random 4-digit OTP
        int randomOtp = 1000 + new java.util.Random().nextInt(9000);
        pdf.setOtpCode(String.valueOf(randomOtp));

        sseService.sendProgress(pdf.getUserId(), pdf.getOrderId(), "CANCEL_WINDOW", "Cancel window active");
        sseService.sendQueueUpdate(pdf.getUserId());

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
            try {
                if (userService.userExists(pdf.getUserId())) {
                    userService.creditWallet(
                            pdf.getUserId(),
                            refundAmount,
                            "REFUND",
                            "Auto-refund: " + reason
                    );
                } else {
                    System.err.println("Could not refund order " + pdf.getOrderId() + " because user " + pdf.getUserId() + " does not exist.");
                }
            } catch (Exception e) {
                System.err.println("Could not refund order " + pdf.getOrderId() + " to user " + pdf.getUserId() + ": " + e.getMessage());
            }
        }

        pdf.setStatus("CANCELLED");
        pdf.setPaymentStatus("REFUNDED");
        pdf.setFinishedAt(LocalDateTime.now());
        pdf.setPdfData(null);
        pdf.setPrintProgress("Cancelled: " + reason);

        repository.save(pdf);
        sseService.sendProgress(pdf.getUserId(), pdf.getOrderId(), "CANCELLED", "Cancelled: " + reason);
        sseService.sendQueueUpdate(pdf.getUserId());
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

    private static final java.util.Map<String, LocalDateTime> agentHeartbeats = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, Boolean> blockOnlineState = new java.util.concurrent.ConcurrentHashMap<>();

    @Scheduled(fixedRate = 30000)
    public void monitorPrinterConnectivity() {
        try {
            List<com.saipraveen.login_registration.entity.PrinterConfig> printers = printerConfigService.getAllPrinters();
            if (printers == null) return;
            for (com.saipraveen.login_registration.entity.PrinterConfig printer : printers) {
                String block = printer.getBlockLocation();
                boolean isOnline = isAgentOnline(block);
                Boolean previousState = blockOnlineState.get(block);

                if (previousState == null) {
                    blockOnlineState.put(block, isOnline);
                    continue;
                }

                if (previousState && !isOnline) {
                    blockOnlineState.put(block, false);
                    emailService.sendEmail(
                        "saipraveendasari2@gmail.com",
                        "CRITICAL ALERT: Printer at " + block + " Went Offline",
                        "Technician Alert:\n\nThe cloud print agent for printer block " + block + 
                        " has stopped responding to heartbeats and is now OFFLINE.\n\n" +
                        "Last Heartbeat: " + agentHeartbeats.get(block) + "\n\n" +
                        "Please verify the print agent service and local network connection at that block."
                    );
                } else if (!previousState && isOnline) {
                    blockOnlineState.put(block, true);
                    emailService.sendEmail(
                        "saipraveendasari2@gmail.com",
                        "RESOLVED: Printer at " + block + " is Back Online",
                        "Technician Update:\n\nThe cloud print agent for printer block " + block + 
                        " has successfully reconnected to the server and is now ONLINE."
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Error running monitorPrinterConnectivity: " + e.getMessage());
        }
    }

    public void recordHeartbeat(String blockLocation) {
        if (blockLocation != null) {
            String normalized = normalizeBlock(blockLocation);
            agentHeartbeats.put(normalized, LocalDateTime.now());
        }
    }

    public boolean isAgentOnline(String blockLocation) {
        if (blockLocation == null) {
            return false;
        }
        String normalized = normalizeBlock(blockLocation);
        LocalDateTime lastHeartbeat = agentHeartbeats.get(normalized);
        if (lastHeartbeat == null) {
            return false;
        }
        return lastHeartbeat.isAfter(LocalDateTime.now().minusSeconds(15));
    }

    @Transactional
    public PdfFile updateProgress(String orderId, String progressMessage) {
        PdfFile pdf = repository.findByOrderId(orderId);
        if (pdf == null) {
            throw new RuntimeException("Order not found");
        }
        pdf.setPrintProgress(progressMessage);
        PdfFile saved = repository.save(pdf);
        sseService.sendProgress(pdf.getUserId(), pdf.getOrderId(), pdf.getStatus(), progressMessage);
        return saved;
    }

    public Map<String, Object> getQueueEstimate(String orderId) {
        PdfFile targetPdf = repository.findByOrderId(orderId);
        Map<String, Object> response = new HashMap<>();
        if (targetPdf == null) {
            response.put("error", "Order not found");
            return response;
        }

        String block = targetPdf.getBlockLocation();
        List<PdfFile> activeQueue = repository.findActiveQueueByBlock(block);

        int position = 0;
        int totalPagesAhead = 0;
        boolean found = false;

        for (int i = 0; i < activeQueue.size(); i++) {
            PdfFile pdf = activeQueue.get(i);
            if (pdf.getOrderId().equals(orderId)) {
                position = i + 1;
                found = true;
                break;
            }
            int pages = (pdf.getTotalPages() != null ? pdf.getTotalPages() : 1) * (pdf.getCopies() != null ? pdf.getCopies() : 1);
            totalPagesAhead += pages;
        }

        if (!found) {
            position = activeQueue.size() + 1;
            for (PdfFile pdf : activeQueue) {
                int pages = (pdf.getTotalPages() != null ? pdf.getTotalPages() : 1) * (pdf.getCopies() != null ? pdf.getCopies() : 1);
                totalPagesAhead += pages;
            }
        }

        double pagePrintTime = 3.0; // 3 seconds per page
        double transferBuffer = 10.0; // 10 seconds transfer buffer per order
        
        double estimatedWaitTime = (totalPagesAhead * pagePrintTime) + (position * transferBuffer);

        response.put("orderId", orderId);
        response.put("queuePosition", position);
        response.put("totalPagesAhead", totalPagesAhead);
        response.put("estimatedWaitTimeSeconds", estimatedWaitTime);
        response.put("estimatedWaitTimeMinutes", Math.ceil(estimatedWaitTime / 60.0 * 10.0) / 10.0);
        return response;
    }
}
