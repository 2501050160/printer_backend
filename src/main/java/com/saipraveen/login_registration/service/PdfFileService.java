package com.saipraveen.login_registration.service;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.printing.PDFPageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.saipraveen.login_registration.entity.PdfFile;
import com.saipraveen.login_registration.entity.User;
import com.saipraveen.login_registration.repository.PdfFileRepository;
import com.saipraveen.login_registration.repository.UserRepository;

@Service
public class PdfFileService {
@Autowired
private QueueService queueService;

@Autowired
private UserService userService;

@Autowired
private SseService sseService;

@Autowired
private PdfFileRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private SystemSettingService systemSettingService;

    @jakarta.annotation.PostConstruct
    public void initSystemSettings() {
        initSetting("referral_enabled", "true");
        initSetting("referral_referrer_amount", "10.0");
        initSetting("referral_referee_amount", "5.0");
        initSetting("referral_popup_enabled", "true");
        initSetting("referral_popup_message", "Welcome! Share your referral code with friends. They get Rs. 5 and you get Rs. 10 on their first checkout!");
        initSetting("ad_enabled", "true");
        initSetting("ad_text", "📢 REFERRAL SPECIAL: Refer your friends using your unique Referral Code shown below and earn ₹10 instantly when they checkout! They get ₹5 off on their first order!");
    }

    private void initSetting(String key, String defaultValue) {
        if (systemSettingService.getSetting(key, null) == null) {
            systemSettingService.setSetting(key, defaultValue);
        }
    }

public PdfFile savePdf(
        MultipartFile file,
        Long userId,
        String customerName,
        String blockLocation)
        throws IOException {

    PdfFile pdf = new PdfFile();

    // User Information
    pdf.setUserId(userId);
    pdf.setCustomerName(
            resolveCustomerName(
                    userId,
                    customerName
            )
    );
    pdf.setBlockLocation(
            normalizeBlockLocation(
                    blockLocation
            )
    );

    // Default values
    pdf.setCopies(1);
    pdf.setSelectedPages("ALL");

    // Generate Sequential Order ID
    Long lastId = repository.getLastId() + 1;

    String orderId =
            "ORD2026" +
            String.format("%04d", lastId);

    pdf.setOrderId(orderId);

    // Upload Time
    pdf.setUploadTime(LocalDateTime.now());

    // File Details
    byte[] fileBytes = file.getBytes();
    String contentType = file.getContentType();
    String filename = file.getOriginalFilename();
    boolean isImage = false;
    if (filename != null) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")) {
            isImage = true;
        }
    }

    if (isImage) {
        fileBytes = convertImageToPdf(fileBytes, contentType);
        pdf.setFileType("application/pdf");
        if (filename != null && !filename.toLowerCase().endsWith(".pdf")) {
            pdf.setFileName(filename + ".pdf");
        } else {
            pdf.setFileName(filename);
        }
    } else {
        pdf.setFileType(contentType);
        pdf.setFileName(filename);
    }
    pdf.setFileSize((long) fileBytes.length);
    pdf.setPdfData(fileBytes);

    // Calculate PDF Page Count
    try (PDDocument document =
                 Loader.loadPDF(fileBytes)) {

        int pageCount =
                document.getNumberOfPages();

        pdf.setTotalPages(pageCount);

        System.out.println(
                "Total Pages = " + pageCount);
    }

pdf.setStatus(
        "ORDER_CREATED"
);

pdf.setPaymentStatus("UNPAID");

    return repository.save(pdf);
}

private byte[] convertImageToPdf(byte[] imageBytes, String contentType) throws IOException {
    try (PDDocument document = new PDDocument();
         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
        document.addPage(page);

        PDImageXObject pdImage = (contentType != null && contentType.contains("png"))
                ? LosslessFactory.createFromImage(document, image)
                : JPEGFactory.createFromImage(document, image);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.drawImage(pdImage, 0, 0);
        }
        document.save(baos);
        return baos.toByteArray();
    }
}

public PdfFile updateOrder(
        String orderId,
        Integer copies,
        String selectedPages,
        String printType,
        String blockLocation) {

    PdfFile pdf =
            repository.findByOrderId(orderId);

    if (pdf == null) {
        throw new RuntimeException(
                "Order not found");
    }

    pdf.setCopies(copies);
    pdf.setSelectedPages(selectedPages);
    pdf.setPrintType(printType);
    pdf.setBlockLocation(
            normalizeBlockLocation(
                    blockLocation
            )
    );

    int pages = calculateSelectedPageCount(selectedPages, pdf.getTotalPages() != null ? pdf.getTotalPages() : 1);

    Double rate = pricingService.getPrice(printType, pdf.getBlockLocation());
    if (rate == null || rate == 0.0) {
        rate = printType.equals("COLOR") ? 5.0 : 2.0;
    }

    double price =
            pages *
            copies *
            rate;

    pdf.setPrice(price);
    pdf.setOriginalPrice(price);
    pdf.setDiscountAmount(0.0);

    return repository.save(pdf);
}

public PdfFile updateStatus(
        Long id,
        String status) {

    PdfFile pdf =
            repository.findById(id)
                    .orElseThrow(
                            () -> new RuntimeException(
                                    "Order Not Found"
                            )
                    );

    pdf.setStatus(status);

    if ("COMPLETED".equals(status)) {
        pdf.setFinishedAt(LocalDateTime.now());
    }

    if ("PRINTING".equals(status) && pdf.getPrintingStartedAt() == null) {
        pdf.setPrintingStartedAt(LocalDateTime.now());
    }

    return repository.save(pdf);
}


public PdfFile updatePaymentStatus(
        Long id,
        String paymentStatus
) {

    PdfFile pdf =
            repository.findById(id)
            .orElseThrow(
                    () -> new RuntimeException(
                            "Order Not Found"
                    )
            );

    pdf.setPaymentStatus(
            paymentStatus
    );

    return repository.save(
            pdf
    );
}
    // Update payment status using order ID (used by Razorpay webhook)
    public PdfFile updatePaymentStatusByOrderId(String orderId, String paymentStatus) {
        PdfFile pdf = repository.findByOrderId(orderId);
        if (pdf == null) {
            throw new RuntimeException("Order Not Found");
        }
        pdf.setPaymentStatus(paymentStatus);
        return repository.save(pdf);
    }


public List<PdfFile> getAllOrders() {

    return repository.findAll();
}

public Map<String,Object> getDashboardStats(String period) {

    Map<String,Object> stats =
            new HashMap<>();

    LocalDateTime start = resolvePeriodStart(period);

    Double grossRevenue;
    Double totalDiscounts;
    Double netRevenue;

    if (start == null) {
        grossRevenue = repository.getGrossRevenueAll();
        totalDiscounts = repository.getTotalDiscountsAll();
        netRevenue = repository.getNetRevenueAll();
    } else {
        grossRevenue = repository.getGrossRevenueSince(start);
        totalDiscounts = repository.getTotalDiscountsSince(start);
        netRevenue = repository.getNetRevenueSince(start);
    }

    stats.put("period", period);
    stats.put("grossRevenue", grossRevenue == null ? 0.0 : grossRevenue);
    stats.put("totalDiscounts", totalDiscounts == null ? 0.0 : totalDiscounts);
    stats.put("netRevenue", netRevenue == null ? 0.0 : netRevenue);
    stats.put("totalRevenue", netRevenue == null ? 0.0 : netRevenue);

    stats.put(
            "todayRevenue",
            repository.getTodayRevenue()
    );

    stats.put(
            "completedOrders",
            repository.getCompletedOrders()
    );

    stats.put(
            "printingOrders",
            repository.getPrintingOrders()
    );

    stats.put(
            "totalOrders",
            repository.getTotalOrders()
    );

    stats.put(
            "totalPages",
            repository.getTotalPagesPrinted()
    );

    stats.put(
            "pendingOrders",
            repository.getPendingOrders()
    );

    return stats;
}

private LocalDateTime resolvePeriodStart(String period) {

    if (period == null || period.isBlank() || "all".equalsIgnoreCase(period)) {
        return null;
    }

    LocalDateTime now = LocalDateTime.now();

    return switch (period.toLowerCase()) {
        case "today" -> now.toLocalDate().atStartOfDay();
        case "week" -> now.minusDays(7);
        case "month" -> now.minusDays(30);
        default -> null;
    };
}


public PdfFile getPdfById(
        Long id) {

    return repository.findById(id)
            .orElseThrow(
                    () ->
                            new RuntimeException(
                                    "PDF Not Found"
                            )
            );
}

public byte[] getPrintablePdfData(PdfFile pdf) {
    if (pdf.getPdfData() == null) {
        return null;
    }
    if (pdf.getSelectedPages() == null || "ALL".equalsIgnoreCase(pdf.getSelectedPages().trim())) {
        return pdf.getPdfData();
    }
    try (PDDocument document = Loader.loadPDF(pdf.getPdfData())) {
        try (PDDocument filteredDoc = createPrintableDocument(document, pdf.getSelectedPages())) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            filteredDoc.save(out);
            return out.toByteArray();
        }
    } catch (Exception e) {
        e.printStackTrace();
        return pdf.getPdfData(); // Fallback to original document on error
    }
}




public List<PdfFile> getUserOrders(
        Long userId
) {

    return repository.findByUserId(
            userId
    );
}

public PdfFile markAsPaid(
        String orderId,
        String paymentId
) {

    PdfFile pdf =
            repository.findByOrderId(
                    orderId
            );

    if (pdf == null) {

        throw new RuntimeException(
                "Order Not Found"
        );
    }

    pdf.setRazorpayPaymentId(
            paymentId
    );

    processReferralRewards(pdf);

    queueService.beginCancelWindow(pdf);

    return repository.save(pdf);
}

public PdfFile payWithWallet(String orderId) {

    PdfFile pdf =
            repository.findByOrderId(orderId);

    if (pdf == null) {
        throw new RuntimeException("Order Not Found");
    }

    if (!"UNPAID".equals(pdf.getPaymentStatus())) {
        throw new RuntimeException("Order already paid");
    }

    Double price =
            pdf.getPrice() == null ? 0.0 : pdf.getPrice();

    userService.debitWallet(pdf.getUserId(), price, "PAYMENT", "Payment for order: " + pdf.getOrderId());

    pdf.setRazorpayPaymentId("WALLET");

    processReferralRewards(pdf);

    queueService.beginCancelWindow(pdf);

    return repository.save(pdf);
}

public PdfFile updateFinalPrice(
        String orderId,
        Double price,
        Double originalPrice,
        Double discountAmount
) {

    PdfFile pdf = repository.findByOrderId(orderId);

    if (pdf == null) {
        throw new RuntimeException("Order Not Found");
    }

    if (!"UNPAID".equals(pdf.getPaymentStatus())) {
        throw new RuntimeException("Order already paid");
    }

    pdf.setPrice(price);

    if (originalPrice != null) {
        pdf.setOriginalPrice(originalPrice);
    } else if (pdf.getOriginalPrice() == null) {
        pdf.setOriginalPrice(price);
    }

    pdf.setDiscountAmount(
            discountAmount == null ? 0.0 : discountAmount
    );

    return repository.save(pdf);
}

public Map<String, Object> getCancelWindowInfo(String orderId) {

    PdfFile pdf =
            repository.findByOrderId(orderId);

    Map<String, Object> info = new HashMap<>();

    if (pdf == null) {
        info.put("found", false);
        return info;
    }

    info.put("found", true);
    info.put("orderId", pdf.getOrderId());
    info.put("status", pdf.getStatus());
    info.put("cancelWindowEndsAt", pdf.getCancelWindowEndsAt());
    info.put("cancelWindowSeconds", queueService.getCancelWindowSeconds());

    if (pdf.getCancelWindowEndsAt() != null) {
        long secondsLeft =
                java.time.Duration.between(
                        LocalDateTime.now(),
                        pdf.getCancelWindowEndsAt()
                ).getSeconds();

        info.put("secondsLeft", Math.max(0, secondsLeft));
    }

    return info;
}

private String resolveCustomerName(
        Long userId,
        String fallbackName
) {

    if (userId != null) {

        return userRepository.findById(userId)
                .map(user -> user.getName())
                .orElseGet(() -> cleanCustomerName(fallbackName));
    }

    return cleanCustomerName(fallbackName);
}

private String cleanCustomerName(String name) {

    if (name == null || name.trim().isEmpty()) {
        return "Customer";
    }

    return name.trim();
}

private String normalizeBlockLocation(String blockLocation) {

    if (blockLocation == null
            || blockLocation.trim().isEmpty()) {

        return "C Block";
    }

    return blockLocation.trim();
}

private void autoPrint(PdfFile pdf) {

    if (pdf.getPdfData() == null) {

        System.out.println(
                "PDF data expired for order "
                        + pdf.getOrderId()
        );

        return;
    }

    try (PDDocument document =
                 Loader.loadPDF(pdf.getPdfData())) {

        PDDocument documentToPrint =
                createPrintableDocument(document, pdf.getSelectedPages());

        try (documentToPrint) {

            PrinterJob printerJob =
                    PrinterJob.getPrinterJob();

            printerJob.setJobName(
                    pdf.getOrderId()
                            + " - "
                            + pdf.getFileName()
            );

            Integer copies =
                    pdf.getCopies();

            printerJob.setCopies(
                    copies == null || copies < 1
                            ? 1
                            : copies
            );

            printerJob.setPageable(
                    new PDFPageable(documentToPrint)
            );

            printerJob.print();

            System.out.println(
                    "PRINT STARTED FOR ORDER "
                            + pdf.getOrderId()
            );
        }

    } catch (IOException | PrinterException e) {

        e.printStackTrace();
    }
}

private PDDocument createPrintableDocument(
        PDDocument sourceDocument,
        String selectedPages
) throws IOException {

    if (selectedPages == null
            || selectedPages.trim().isEmpty()
            || "ALL".equalsIgnoreCase(selectedPages.trim())) {

        PDDocument copy =
                new PDDocument();

        for (int pageIndex = 0;
             pageIndex < sourceDocument.getNumberOfPages();
             pageIndex++) {

            copy.importPage(
                    sourceDocument.getPage(pageIndex)
            );
        }

        return copy;
    }

    PDDocument selectedDocument =
            new PDDocument();

    String[] parts =
            selectedPages.split(",");

    for (String part : parts) {

        String pagePart =
                part.trim();

        if (pagePart.isEmpty()) {
            continue;
        }

        if (pagePart.contains("-")) {

            String[] range =
                    pagePart.split("-");

            int startPage =
                    Integer.parseInt(range[0].trim());

            int endPage =
                    Integer.parseInt(range[1].trim());

            addPageRange(
                    sourceDocument,
                    selectedDocument,
                    startPage,
                    endPage
            );

        } else {

            int pageNumber =
                    Integer.parseInt(pagePart);

            addPageRange(
                    sourceDocument,
                    selectedDocument,
                    pageNumber,
                    pageNumber
            );
        }
    }

    if (selectedDocument.getNumberOfPages() == 0) {

        throw new IOException(
                "No printable pages selected for order "
                        + selectedPages
        );
    }

    return selectedDocument;
}

private void addPageRange(
        PDDocument sourceDocument,
        PDDocument selectedDocument,
        int startPage,
        int endPage
) throws IOException {

    int safeStart =
            Math.max(1, startPage);

    int safeEnd =
            Math.min(
                    sourceDocument.getNumberOfPages(),
                    endPage
            );

    if (safeStart > safeEnd) {
        return;
    }

    for (int pageNumber = safeStart;
         pageNumber <= safeEnd;
         pageNumber++) {

        selectedDocument.importPage(
                sourceDocument.getPage(pageNumber - 1)
        );
    }
}

    @Transactional
    public Map<String, Object> applyReferral(String orderId, String referralCode, Long currentUserId) {
        Map<String, Object> response = new HashMap<>();
        
        // 1. Verify Referral Program is enabled globally
        if (!systemSettingService.getSettingBool("referral_enabled", true)) {
            response.put("success", false);
            response.put("message", "Referral program is currently deactivated");
            return response;
        }

        PdfFile pdf = repository.findByOrderId(orderId);
        if (pdf == null) {
            response.put("success", false);
            response.put("message", "Order not found");
            return response;
        }

        if (pdf.getAppliedReferralCode() != null) {
            response.put("success", false);
            response.put("message", "Referral code already applied");
            return response;
        }

        User referrer = userRepository.findByReferralCode(referralCode.trim());
        if (referrer == null) {
            response.put("success", false);
            response.put("message", "Invalid referral code");
            return response;
        }

        if (referrer.getId().equals(currentUserId)) {
            response.put("success", false);
            response.put("message", "You cannot refer yourself");
            return response;
        }

        // 2. Verify user is on their first order
        long paidOrders = repository.countByUserIdAndPaymentStatus(currentUserId, "PAID");
        if (paidOrders > 0) {
            response.put("success", false);
            response.put("message", "Referral codes can only be applied to your first order.");
            return response;
        }

        pdf.setAppliedReferralCode(referralCode.trim());
        repository.save(pdf);

        response.put("success", true);
        response.put("message", "Referral code applied successfully! Rewards will be credited upon payment.");
        return response;
    }

    private void processReferralRewards(PdfFile pdf) {
        if (!systemSettingService.getSettingBool("referral_enabled", true)) {
            return;
        }
        String refCode = pdf.getAppliedReferralCode();
        if (refCode != null && !refCode.trim().isEmpty()) {
            try {
                User referrer = userRepository.findByReferralCode(refCode.trim());
                if (referrer != null && !referrer.getId().equals(pdf.getUserId())) {
                    double referrerAmt = systemSettingService.getSettingDouble("referral_referrer_amount", 10.0);
                    double refereeAmt = systemSettingService.getSettingDouble("referral_referee_amount", 5.0);

                    // Credit referrer
                    userService.creditWallet(referrer.getId(), referrerAmt, "REWARD", "Referral reward for referring order " + pdf.getOrderId());
                    // Credit referee (current order user)
                    userService.creditWallet(pdf.getUserId(), refereeAmt, "REWARD", "Referral signup bonus");
                    System.out.println("Applied referral code: " + refCode + ". Referrer " + referrer.getId() + " credited " + referrerAmt + ", referee " + pdf.getUserId() + " credited " + refereeAmt + ".");
                }
            } catch (Exception e) {
                System.err.println("Failed to apply referral code rewards: " + e.getMessage());
            }
        }
    }

    @Transactional
    public void resetAllStats() {
        repository.deleteAll();
    }

    public PdfFile saveMultiplePdfs(
            MultipartFile[] files,
            Long userId,
            String customerName,
            String blockLocation
    ) throws IOException {
        java.util.List<byte[]> pdfBytesList = new java.util.ArrayList<>();
        String combinedName = "";
        
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            byte[] fileBytes = file.getBytes();
            String filename = file.getOriginalFilename();
            String contentType = file.getContentType();
            
            boolean isImage = false;
            if (filename != null) {
                String lower = filename.toLowerCase();
                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")) {
                    isImage = true;
                }
            }
            
            if (isImage) {
                fileBytes = convertImageToPdf(fileBytes, contentType);
            }
            
            pdfBytesList.add(fileBytes);
            
            String baseName = filename != null ? filename : "Document";
            if (isImage && !baseName.toLowerCase().endsWith(".pdf")) {
                baseName = baseName + ".pdf";
            }
            
            if (i == 0) {
                combinedName = baseName;
            } else if (i == 1) {
                combinedName = combinedName + " + " + baseName;
            }
        }
        
        if (files.length > 2) {
            combinedName = combinedName + " (and " + (files.length - 2) + " more)";
        }
        
        byte[] mergedPdfBytes = mergePdfs(pdfBytesList);
        
        PdfFile pdf = new PdfFile();
        pdf.setUserId(userId);
        pdf.setCustomerName(resolveCustomerName(userId, customerName));
        pdf.setBlockLocation(normalizeBlockLocation(blockLocation));
        pdf.setCopies(1);
        pdf.setSelectedPages("ALL");
        
        Long lastId = repository.getLastId() + 1;
        String orderId = "ORD2026" + String.format("%04d", lastId);
        pdf.setOrderId(orderId);
        pdf.setUploadTime(LocalDateTime.now());
        pdf.setFileName(combinedName);
        pdf.setFileType("application/pdf");
        pdf.setFileSize((long) mergedPdfBytes.length);
        pdf.setPdfData(mergedPdfBytes);
        
        try (PDDocument document = Loader.loadPDF(mergedPdfBytes)) {
            pdf.setTotalPages(document.getNumberOfPages());
        }
        
        pdf.setStatus("ORDER_CREATED");
        pdf.setPaymentStatus("UNPAID");
        
        return repository.save(pdf);
    }

    private byte[] mergePdfs(java.util.List<byte[]> pdfBytesList) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            merger.setDestinationStream(baos);
            for (byte[] bytes : pdfBytesList) {
                merger.addSource(new org.apache.pdfbox.io.RandomAccessReadBuffer(bytes));
            }
            merger.mergeDocuments(null);
            return baos.toByteArray();
        }
    }

    public List<PdfFile> getPendingScanOrders(Long userId, String blockLocation) {
        return repository.findByUserIdAndBlockLocationAndStatus(userId, blockLocation, "PENDING_SCAN");
    }

    @Transactional
    public PdfFile releasePrintJob(String orderId, String otp) {
        PdfFile pdf = repository.findByOrderId(orderId);
        if (pdf == null) {
            throw new RuntimeException("Order Not Found");
        }
        if (!"PENDING_SCAN".equals(pdf.getStatus())) {
            throw new RuntimeException("Order is not in PENDING_SCAN state");
        }
        
        // OTP check is bypassed for seamless Scan-to-Print QR release
        pdf.setStatus("QUEUE");
        pdf.setQueuedAt(LocalDateTime.now());
        pdf.setPrintProgress("Queued for printing");
        PdfFile saved = repository.save(pdf);
        
        sseService.sendProgress(pdf.getUserId(), pdf.getOrderId(), "QUEUE", "Queued for printing");
        sseService.sendQueueUpdate(pdf.getUserId());
        
        return saved;
    }

    public byte[] generateReceiptPdf(String orderId) throws IOException {
        PdfFile pdf = repository.findByOrderId(orderId);
        if (pdf == null) {
            return null;
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Draw header
                contentStream.beginText();
                contentStream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD), 20);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("CAMPUS IOT PRINT HUB");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 10);
                contentStream.newLineAtOffset(50, 730);
                contentStream.showText("Official Print Receipt & Invoice");
                contentStream.endText();

                // Draw horizontal line
                contentStream.setLineWidth(1f);
                contentStream.moveTo(50, 715);
                contentStream.lineTo(550, 715);
                contentStream.stroke();

                // Details
                int y = 680;
                contentStream.beginText();
                contentStream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.newLineAtOffset(50, y);
                contentStream.showText("Invoice Details:");
                contentStream.endText();

                y -= 20;
                String[][] details = {
                    {"Order ID:", pdf.getOrderId()},
                    {"Date:", pdf.getUploadTime() != null ? pdf.getUploadTime().toString().substring(0, 10) : "N/A"},
                    {"Customer Name:", pdf.getCustomerName() != null ? pdf.getCustomerName() : "Customer"},
                    {"Location Block:", pdf.getBlockLocation()},
                    {"Document Name:", pdf.getFileName()},
                    {"Print Type:", pdf.getPrintType() != null ? pdf.getPrintType() : "BW"},
                    {"Pages Printed:", pdf.getSelectedPages()},
                    {"Total Pages:", pdf.getTotalPages() != null ? String.valueOf(pdf.getTotalPages()) : "1"},
                    {"Copies:", pdf.getCopies() != null ? String.valueOf(pdf.getCopies()) : "1"},
                    {"Payment Method:", pdf.getRazorpayPaymentId() != null ? pdf.getRazorpayPaymentId() : "UNPAID"},
                    {"Amount Paid:", "Rs. " + (pdf.getPrice() != null ? String.valueOf(pdf.getPrice()) : "0.0")}
                };

                for (String[] detail : details) {
                    contentStream.beginText();
                    contentStream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                    contentStream.newLineAtOffset(50, y);
                    contentStream.showText(detail[0]);
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 10);
                    contentStream.newLineAtOffset(200, y);
                    String value = detail[1] != null ? detail[1] : "";
                    // Replace special characters to avoid PDFBox rendering errors
                    value = value.replaceAll("[^\\x20-\\x7E]", "");
                    if (value.length() > 50) {
                        value = value.substring(0, 47) + "...";
                    }
                    contentStream.showText(value);
                    contentStream.endText();

                    y -= 20;
                }

                // Draw signature/footer
                contentStream.setLineWidth(0.5f);
                contentStream.moveTo(50, 150);
                contentStream.lineTo(550, 150);
                contentStream.stroke();

                contentStream.beginText();
                contentStream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_OBLIQUE), 9);
                contentStream.newLineAtOffset(50, 130);
                contentStream.showText("This is an electronically generated document. No signature required.");
                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    public static int calculateSelectedPageCount(String selectedPages, int totalPages) {
        if ("ALL".equalsIgnoreCase(selectedPages) || selectedPages == null || selectedPages.trim().isEmpty()) {
            return totalPages;
        }
        int count = 0;
        String[] parts = selectedPages.split(",");
        for (String part : parts) {
            String pagePart = part.trim();
            if (pagePart.isEmpty()) continue;
            if (pagePart.contains("-")) {
                String[] range = pagePart.split("-");
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());
                        count += Math.max(0, end - start + 1);
                    } catch (NumberFormatException e) {
                        count += 1;
                    }
                }
            } else {
                try {
                    Integer.parseInt(pagePart);
                    count += 1;
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return count > 0 ? count : 1;
    }
}
