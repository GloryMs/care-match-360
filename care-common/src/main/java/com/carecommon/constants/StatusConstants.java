package com.carecommon.constants;

public class StatusConstants {
    // Offer Status
    public static final String OFFER_DRAFT = "DRAFT";
    public static final String OFFER_SENT = "SENT";
    public static final String OFFER_VIEWED = "VIEWED";
    public static final String OFFER_ACCEPTED = "ACCEPTED";
    public static final String OFFER_REJECTED = "REJECTED";
    public static final String OFFER_EXPIRED = "EXPIRED";

    // Subscription Status
    public static final String SUBSCRIPTION_ACTIVE = "ACTIVE";
    public static final String SUBSCRIPTION_PAUSED = "PAUSED";
    public static final String SUBSCRIPTION_CANCELLED = "CANCELLED";

    // Invoice Status
    public static final String INVOICE_PENDING = "PENDING";
    public static final String INVOICE_PAID = "PAID";
    public static final String INVOICE_FAILED = "FAILED";

    // Notification Status
    public static final String NOTIFICATION_PENDING = "PENDING";
    public static final String NOTIFICATION_SENT = "SENT";
    public static final String NOTIFICATION_FAILED = "FAILED";

    private StatusConstants() {
        // Private constructor to prevent instantiation
    }
}