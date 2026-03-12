package com.careprofileservice.model;

/**
 * Classifies the care services offered by a provider into tiers.
 * STANDARD   – regulatory-minimum care; accepts GKV fully-covered patients.
 * COMFORT    – enhanced services with selective amenities; partial private-pay.
 * PREMIUM    – luxury / VIP services; full private-pay or high-end supplemental insurance.
 * A provider may offer MULTIPLE tiers (e.g. has both standard and premium wards).
 * Stored as a Set<ProviderServiceTier> on the provider profile.
 */
public enum ProviderServiceTier {

    /** Statutory-minimum care — GKV-covered residents/patients accepted. */
    STANDARD,

    /** Comfort-level care with selected premium amenities. */
    COMFORT,

    /** Full VIP / luxury care packages with personalised concierge services. */
    PREMIUM
}