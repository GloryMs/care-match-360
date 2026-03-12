package com.careprofileservice.model;

/**
 * Represents the patient's preferred and affordable care service tier.
 *
 * STANDARD   – covers basic statutory (GKV) care services, both residential and ambulatory.
 * COMFORT    – covers standard services plus selected premium amenities
 *              (e.g. single room, extended therapy hours).
 * PREMIUM    – covers full VIP / luxury care packages with personalised services,
 *              private nursing staff, superior accommodation, and lifestyle concierge.
 *
 * This field is stored on the patient profile and is visible to all subscribed providers
 * so they can proactively search for patients whose service tier matches their offering.
 */
public enum CareServiceTier {

    /** Basic statutory care — minimum monthly co-payment expected from patient. */
    STANDARD,

    /** Mid-range comfort care — moderate private top-up payments expected. */
    COMFORT,

    /** Full premium / VIP care — highest private-pay capacity. */
    PREMIUM
}