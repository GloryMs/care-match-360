package com.carebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Full result returned by the demo subscription simulation.
 * Bundles the created subscription, the first invoice, and the payment record
 * so the front-end can display all three in one API call.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoSubscriptionResult {

    /** The created (or existing) subscription. */
    private SubscriptionResponse subscription;

    /** The invoice generated for this billing cycle. */
    private InvoiceResponse invoice;

    /** The payment attempt record (SUCCEEDED or FAILED based on simulatePaymentFailure). */
    private PaymentHistoryResponse payment;

    /** Human-readable summary of what was simulated. */
    private String simulationSummary;
}
