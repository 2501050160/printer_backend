package com.saipraveen.login_registration.service;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
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
private PdfFileRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PricingService pricingService;

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
    pdf.setFileName(file.getOriginalFilename());
    pdf.setFileType(file.getContentType());
    pdf.setFileSize(file.getSize());
    pdf.setPdfData(file.getBytes());

    // Calculate PDF Page Count
    try (PDDocument document =
                 Loader.loadPDF(file.getBytes())) {

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

    int pages = 1;

    if ("ALL".equals(selectedPages) || selectedPages == null || selectedPages.trim().isEmpty()) {
        pages = pdf.getTotalPages() != null ? pdf.getTotalPages() : 1;
    } else {
        try {
            String[] range = selectedPages.split("-");
            if (range.length == 2) {
                pages = Integer.parseInt(range[1].trim())
                        - Integer.parseInt(range[0].trim())
                        + 1;
            } else if (range.length == 1) {
                pages = 1;
            }
        } catch (Exception e) {
            pages = 1;
        }
    }

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

    userService.debitWallet(pdf.getUserId(), price);

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

        pdf.setAppliedReferralCode(referralCode.trim());
        repository.save(pdf);

        response.put("success", true);
        response.put("message", "Referral code applied successfully! Rewards will be credited upon payment.");
        return response;
    }

    private void processReferralRewards(PdfFile pdf) {
        String refCode = pdf.getAppliedReferralCode();
        if (refCode != null && !refCode.trim().isEmpty()) {
            try {
                User referrer = userRepository.findByReferralCode(refCode.trim());
                if (referrer != null && !referrer.getId().equals(pdf.getUserId())) {
                    // Credit referrer ₹10
                    userService.creditWallet(referrer.getId(), 10.0);
                    // Credit referee (current order user) ₹5
                    userService.creditWallet(pdf.getUserId(), 5.0);
                    System.out.println("Applied referral code: " + refCode + ". Referrer " + referrer.getId() + " credited 10, referee " + pdf.getUserId() + " credited 5.");
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
}
