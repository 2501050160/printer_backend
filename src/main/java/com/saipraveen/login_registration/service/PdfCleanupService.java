package com.saipraveen.login_registration.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saipraveen.login_registration.repository.PdfFileRepository;

@Service
public class PdfCleanupService {

    @Autowired
    private PdfFileRepository repository;

    @Value("${print.pdf.retention-minutes:30}")
    private int retentionMinutes;

    @Value("${print.pdf.daily-cleanup-enabled:true}")
    private boolean dailyCleanupEnabled;

    @Scheduled(fixedRate = 60 * 1000, initialDelay = 30 * 1000)
    @Transactional
    public void removePdfDataAfterRetention() {

        LocalDateTime cutoff =
                LocalDateTime.now()
                        .minusMinutes(retentionMinutes);

        int cleanedCount =
                repository.clearPdfDataFinishedBefore(
                        cutoff
                );

        if (cleanedCount > 0) {

            System.out.println(
                    "Removed PDF data from "
                            + cleanedCount
                            + " finished order(s) older than "
                            + retentionMinutes
                            + " minutes"
            );
        }
    }

    @Scheduled(cron = "${print.pdf.daily-cleanup-cron:0 0 2 * * *}")
    @Transactional
    public void dailyPdfDataReset() {

        if (!dailyCleanupEnabled) {
            return;
        }

        int finishedCount =
                repository.clearPdfDataForFinishedOrders();

        int unpaidCount =
                repository.clearPdfDataForUnpaidOlderThan(
                        LocalDateTime.now().minusDays(1)
                );

        int total = finishedCount + unpaidCount;

        if (total > 0) {

            System.out.println(
                    "Daily PDF cleanup: removed data from "
                            + finishedCount
                            + " finished and "
                            + unpaidCount
                            + " stale unpaid order(s)"
            );
        }
    }
}
