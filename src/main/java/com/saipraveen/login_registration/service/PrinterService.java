package com.saipraveen.login_registration.service;

import java.io.File;
import java.io.FileOutputStream;

import org.springframework.stereotype.Service;

import com.saipraveen.login_registration.entity.PdfFile;

@Service
public class PrinterService {

    public void printPdf(PdfFile pdf) {

        try {

            File tempFile =
                    File.createTempFile(
                            "order_",
                            ".pdf"
                    );

            FileOutputStream fos =
                    new FileOutputStream(
                            tempFile
                    );

            fos.write(
                    pdf.getPdfData()
            );

            fos.close();

            System.out.println(
                    "PDF SAVED : "
                            + tempFile.getAbsolutePath()
            );

            Runtime.getRuntime().exec(
                    "cmd /c start /min acrord32 /t \""
                            + tempFile.getAbsolutePath()
                            + "\""
            );

            System.out.println(
                    "PRINT COMMAND SENT"
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}