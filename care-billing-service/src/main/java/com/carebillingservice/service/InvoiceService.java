package com.carebillingservice.service;

import com.carebillingservice.dto.InvoiceResponse;
import com.carebillingservice.dto.SubscriptionPlanInfo;
import com.carebillingservice.model.Invoice;
import com.carebillingservice.model.Subscription;
import com.carebillingservice.repository.InvoiceRepository;
import com.carebillingservice.repository.SubscriptionRepository;
import com.carecommon.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoicePdfService invoicePdfService;

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID invoiceId) {
        ModelMapper mapper = new ModelMapper();
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));
        InvoiceResponse  invoiceResponse = new InvoiceResponse();
        invoiceResponse =  mapper.map(invoice, InvoiceResponse.class);
        return invoiceResponse;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
        ModelMapper mapper = new ModelMapper();
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "invoiceNumber", invoiceNumber));
        InvoiceResponse  invoiceResponse = new InvoiceResponse();
        invoiceResponse =  mapper.map(invoice, InvoiceResponse.class);
        return invoiceResponse;
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesForSubscription(UUID subscriptionId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);

        Page<Invoice> invoicesPage = invoiceRepository.findBySubscriptionIdOrderByIssuedAtDesc(
                subscriptionId, pageable);

        ModelMapper modelMapper = new ModelMapper();
        return invoicesPage.getContent().stream()
                .map(invoice -> modelMapper.map(invoice, InvoiceResponse.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public String generateInvoicePdf(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        Subscription subscription = subscriptionRepository.findById(invoice.getSubscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", invoice.getSubscriptionId()));

        // Generate PDF
        String pdfPath = invoicePdfService.generateInvoicePdf(invoice, subscription);

        // Update invoice with PDF path
        invoice.setPdfUrl(pdfPath);
        invoiceRepository.save(invoice);

        log.info("Invoice PDF generated and saved: invoiceId={}, path={}", invoiceId, pdfPath);

        return pdfPath;
    }

    @Transactional
    public Invoice createInvoice(UUID subscriptionId, String stripeInvoiceId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", subscriptionId));

        SubscriptionPlanInfo planInfo = getSubscriptionPlanInfo(subscription.getTier());

        String invoiceNumber = generateInvoiceNumber();

        Invoice invoice = Invoice.builder()
                .subscriptionId(subscriptionId)
                .invoiceNumber(invoiceNumber)
                .amount(planInfo.getPrice())
                .currency(planInfo.getCurrency())
                .status(Invoice.InvoiceStatus.PENDING)
                .stripeInvoiceId(stripeInvoiceId)
                .issuedAt(LocalDateTime.now())
                .dueAt(LocalDateTime.now().plusDays(7))
                .build();

        invoice = invoiceRepository.save(invoice);
        log.info("Invoice created: invoiceId={}, invoiceNumber={}", invoice.getId(), invoiceNumber);

        return invoice;
    }

    private String generateInvoiceNumber() {
        // Format: INV-YYYYMMDD-RANDOM
        String datePart = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "INV-" + datePart + "-" + randomPart;
    }

    private SubscriptionPlanInfo getSubscriptionPlanInfo(Subscription.SubscriptionTier tier) {
        // This is a simplified version - in production, fetch from SubscriptionPlanConfig
        return SubscriptionPlanInfo.builder()
                .name(tier.name())
                .price(java.math.BigDecimal.valueOf(tier == Subscription.SubscriptionTier.BASIC ? 49.99 :
                        tier == Subscription.SubscriptionTier.PRO ? 99.99 : 199.99))
                .currency("EUR")
                .interval("month")
                .build();
    }
}
