package com.carebillingservice.service;

import com.carebillingservice.model.Invoice;
import com.carebillingservice.model.Subscription;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class InvoicePdfService {

    @Value("${app.invoice.storage-path}")
    private String invoiceStoragePath;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public String generateInvoicePdf(Invoice invoice, Subscription subscription) {
        try {
            // Ensure directory exists
            Path directory = Paths.get(invoiceStoragePath);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            // Create PDF file
            String fileName = "invoice_" + invoice.getInvoiceNumber() + ".pdf";
            String filePath = invoiceStoragePath + File.separator + fileName;

            PdfWriter writer = new PdfWriter(new FileOutputStream(filePath));
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            // Add header
            document.add(new Paragraph("CareMatch360")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("INVOICE")
                    .setFontSize(16)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            // Add invoice details
            document.add(new Paragraph("Invoice Number: " + invoice.getInvoiceNumber()));
            document.add(new Paragraph("Issue Date: " + invoice.getIssuedAt().format(DATE_FORMATTER)));

            if (invoice.getDueAt() != null) {
                document.add(new Paragraph("Due Date: " + invoice.getDueAt().format(DATE_FORMATTER)));
            }

            document.add(new Paragraph("\n"));

            // Add billing information
            document.add(new Paragraph("Subscription Details")
                    .setFontSize(14)
                    .setBold());

            document.add(new Paragraph("Plan: " + subscription.getTier().name()));
            document.add(new Paragraph("Status: " + invoice.getStatus().name()));

            document.add(new Paragraph("\n"));

            // Add items table
            Table table = new Table(3);
            table.addCell("Description");
            table.addCell("Quantity");
            table.addCell("Amount");

            table.addCell(subscription.getTier().name() + " Subscription");
            table.addCell("1");
            table.addCell(invoice.getAmount() + " " + invoice.getCurrency());

            document.add(table);

            document.add(new Paragraph("\n"));

            // Add total
            document.add(new Paragraph("Total: " + invoice.getAmount() + " " + invoice.getCurrency())
                    .setFontSize(14)
                    .setBold()
                    .setTextAlignment(TextAlignment.RIGHT));

            document.add(new Paragraph("\n\n"));

            // Add footer
            document.add(new Paragraph("Thank you for your business!")
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("CareMatch360 - Connecting Care Providers and Patients")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));

            document.close();

            log.info("Invoice PDF generated: invoiceNumber={}, filePath={}",
                    invoice.getInvoiceNumber(), filePath);

            return filePath;

        } catch (Exception e) {
            log.error("Error generating invoice PDF: invoiceNumber={}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }
}
