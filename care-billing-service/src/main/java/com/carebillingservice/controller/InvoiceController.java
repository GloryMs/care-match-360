package com.carebillingservice.controller;

import com.carebillingservice.dto.InvoiceResponse;
import com.carebillingservice.service.InvoiceService;
import com.carecommon.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
//@Tag(name = "Invoices", description = "Invoice management endpoints")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/{invoiceId}")
    //@Operation(summary = "Get invoice by ID")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(@PathVariable UUID invoiceId) {
        InvoiceResponse invoice = invoiceService.getInvoice(invoiceId);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @GetMapping("/number/{invoiceNumber}")
    //@Operation(summary = "Get invoice by invoice number")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByNumber(
            @PathVariable String invoiceNumber) {

        InvoiceResponse invoice = invoiceService.getInvoiceByNumber(invoiceNumber);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @GetMapping("/subscription/{subscriptionId}")
    //@Operation(summary = "Get invoices for subscription")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoicesForSubscription(
            @PathVariable UUID subscriptionId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        List<InvoiceResponse> invoices = invoiceService.getInvoicesForSubscription(
                subscriptionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    @GetMapping("/{invoiceId}/pdf")
    //@Operation(summary = "Download invoice PDF")
    public ResponseEntity<Resource> downloadInvoicePdf(@PathVariable UUID invoiceId) {
        String pdfPath = invoiceService.generateInvoicePdf(invoiceId);

        File file = new File(pdfPath);
        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
