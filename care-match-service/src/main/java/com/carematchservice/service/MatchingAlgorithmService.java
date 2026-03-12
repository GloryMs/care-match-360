package com.carematchservice.service;

import com.carematchservice.dto.PatientProfileDTO;
import com.carematchservice.dto.ProviderProfileDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Enhanced matching algorithm v2.
 * Updated weight distribution (total = 100%):
 * ┌──────────────────────────────────┬────────────────┬─────────────────┐
 * │ Dimension                        │ Old weight (%) │ New weight (%)  │
 * ├──────────────────────────────────┼────────────────┼─────────────────┤
 * │ Care Level Compatibility         │      30        │      25         │
 * │ Distance                         │      20        │      20         │
 * │ Specialization Match             │      20        │      20         │
 * │ Lifestyle Compatibility          │      20        │      10         │
 * │ Social Compatibility             │      10        │       5         │
 * │ Care Service Tier Compatibility  │       —        │      15  (NEW)  │
 * │ Quality Indicators Bonus         │       —        │       5  (NEW)  │
 * └──────────────────────────────────┴────────────────┴─────────────────┘
 * The tier dimension ensures PREMIUM patients are matched primarily with PREMIUM providers,
 * improving relevance for both parties and reducing offer rejection rates.
 */
@Service
@Slf4j
public class MatchingAlgorithmService {

    // ── existing weight config ────────────────────────────────────────────────
    @Value("${matching.weights.care-level}")
    private int careLevelWeight;

    @Value("${matching.weights.distance}")
    private int distanceWeight;

    @Value("${matching.weights.specialization}")
    private int specializationWeight;

    @Value("${matching.weights.lifestyle}")
    private int lifestyleWeight;

    @Value("${matching.weights.social}")
    private int socialWeight;

    // ── new weight config (add to application.properties) ────────────────────
    // matching.weights.tier=15
    // matching.weights.quality=5
    @Value("${matching.weights.tier:15}")
    private int tierWeight;

    @Value("${matching.weights.quality:5}")
    private int qualityWeight;

    // ─────────────────────────────────────────────────────────────────────────
    // calculateMatchScore — updated: 7 dimensions, total still sums to 100
    //
    // Old weights:  careLevel=30, distance=20, specialization=20, lifestyle=20, social=10
    // New weights:  careLevel=25, distance=20, specialization=20, tier=15, lifestyle=10, social=5, quality=5
    //
    // NOTE: The existing @Value fields for careLevelWeight etc. now read from
    // the updated application.properties values (25, 10, 5). No code change
    // needed here — the math stays the same, the config values change.
    // ─────────────────────────────────────────────────────────────────────────
    public BigDecimal calculateMatchScore(PatientProfileDTO patient, ProviderProfileDTO provider) {
        double totalScore = 0.0;
        Map<String, Double> breakdown = new HashMap<>();

        // 1. Care Level Compatibility (25%)
        double careLevelScore = calculateCareLevelScore(patient, provider);
        totalScore += careLevelScore * careLevelWeight;
        breakdown.put("careLevel", careLevelScore);

        // 2. Distance (20%)
        double distanceScore = calculateDistanceScore(patient, provider);
        totalScore += distanceScore * distanceWeight;
        breakdown.put("distance", distanceScore);

        // 3. Specialization Match (20%)
        double specializationScore = calculateSpecializationScore(patient, provider);
        totalScore += specializationScore * specializationWeight;
        breakdown.put("specialization", specializationScore);

        // 4. Care Service Tier Compatibility (15%) — NEW
        double tierScore = calculateTierScore(patient, provider);
        totalScore += tierScore * tierWeight;
        breakdown.put("tier", tierScore);

        // 5. Lifestyle Compatibility (10%)
        double lifestyleScore = calculateLifestyleScore(patient, provider);
        totalScore += lifestyleScore * lifestyleWeight;
        breakdown.put("lifestyle", lifestyleScore);

        // 6. Social Compatibility (5%)
        double socialScore = calculateSocialScore(patient, provider);
        totalScore += socialScore * socialWeight;
        breakdown.put("social", socialScore);

        // 7. Quality Indicators (5%) — NEW
        double qualityScore = calculateQualityScore(patient, provider);
        totalScore += qualityScore * qualityWeight;
        breakdown.put("quality", qualityScore);

        log.debug("Match score calculated: patient={}, provider={}, score={}, breakdown={}",
                patient.getId(), provider.getId(), totalScore, breakdown);

        return BigDecimal.valueOf(totalScore).setScale(2, RoundingMode.HALF_UP);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // generateExplanation — updated to include tier and quality reasons
    // ─────────────────────────────────────────────────────────────────────────
    public Map<String, Object> generateExplanation(
            PatientProfileDTO patient,
            ProviderProfileDTO provider,
            BigDecimal score) {

        Map<String, Object> explanation = new HashMap<>();
        List<String> strengths  = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();

        // Care Level
        if (patient.getCareLevel() != null) {
            double cs = calculateCareLevelScore(patient, provider);
            if (cs >= 0.8) {
                strengths.add(String.format("Care level %d: provider is fully certified for this Pflegegrad",
                        patient.getCareLevel()));
            } else if (cs >= 0.5) {
                strengths.add(String.format("Care level %d: provider accepts adjacent Pflegegrad",
                        patient.getCareLevel()));
            } else {
                weaknesses.add(String.format("Care level %d: provider may not be certified for this Pflegegrad",
                        patient.getCareLevel()));
            }
        }

        // Distance
        if (patient.getLatitude() != null && patient.getLongitude() != null
                && provider.getLatitude() != null && provider.getLongitude() != null) {
            double distance = calculateDistance(
                    patient.getLatitude(), patient.getLongitude(),
                    provider.getLatitude(), provider.getLongitude());
            String distanceMsg = String.format("Located %.1f km away", distance);
            if (distance <= 25) {
                strengths.add(distanceMsg);
            } else {
                weaknesses.add(distanceMsg);
            }
        }

        // Specializations
        if (patient.getMedicalRequirements() != null && provider.getSpecializations() != null) {
            Set<String> patientNeeds = extractMedicalRequirements(patient.getMedicalRequirements());
            Set<String> providerSpecs = new HashSet<>(provider.getSpecializations());
            Set<String> matches = new HashSet<>(patientNeeds);
            matches.retainAll(providerSpecs);
            if (!matches.isEmpty()) {
                strengths.add("Specialized in: " + String.join(", ", matches));
            }
        }

        // Lifestyle
        if (patient.getLifestyleAttributes() != null) {
            double ls = calculateLifestyleScore(patient, provider);
            if (ls >= 0.7) {
                strengths.add("Lifestyle preferences are well accommodated");
            } else {
                weaknesses.add("Some lifestyle preferences may not be met");
            }
        }

        // Care Service Tier — NEW
        String patientTier = patient.getCareServiceTier() != null
                ? patient.getCareServiceTier() : "STANDARD";
        double ts = calculateTierScore(patient, provider);
        if (ts >= 0.8) {
            strengths.add("Care service tier (" + patientTier + "): excellent match");
        } else if (ts >= 0.5) {
            strengths.add("Care service tier (" + patientTier + "): acceptable match");
        } else {
            weaknesses.add("Care service tier (" + patientTier + "): provider tier may not align with patient expectations");
        }

        // Quality — NEW
        double qs = calculateQualityScore(patient, provider);
        if (qs >= 0.7) {
            strengths.add("Provider has strong quality indicators");
        }

        explanation.put("score",     score);
        explanation.put("strengths", strengths);
        explanation.put("weaknesses",weaknesses);
        explanation.put("summary",   generateSummary(score));

        return explanation;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getScoreBreakdown — updated to include tier and quality entries
    // ─────────────────────────────────────────────────────────────────────────
    public Map<String, Object> getScoreBreakdown(PatientProfileDTO patient, ProviderProfileDTO provider) {
        Map<String, Object> breakdown = new HashMap<>();

        breakdown.put("careLevelScore",      calculateCareLevelScore(patient, provider));
        breakdown.put("distanceScore",       calculateDistanceScore(patient, provider));
        breakdown.put("specializationScore", calculateSpecializationScore(patient, provider));
        breakdown.put("tierScore",           calculateTierScore(patient, provider));       // NEW
        breakdown.put("lifestyleScore",      calculateLifestyleScore(patient, provider));
        breakdown.put("socialScore",         calculateSocialScore(patient, provider));
        breakdown.put("qualityScore",        calculateQualityScore(patient, provider));    // NEW

        breakdown.put("weights", Map.of(
                "careLevel",      careLevelWeight,
                "distance",       distanceWeight,
                "specialization", specializationWeight,
                "tier",           tierWeight,             // NEW
                "lifestyle",      lifestyleWeight,
                "social",         socialWeight,
                "quality",        qualityWeight           // NEW
        ));

        return breakdown;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXISTING private scoring methods — unchanged
    // ─────────────────────────────────────────────────────────────────────────

    private double calculateCareLevelScore(PatientProfileDTO patient, ProviderProfileDTO provider) {
        if (patient.getCareLevel() == null) {
            return 0.5;
        }
        // If provider declares accepted care levels, do an exact/adjacent check
        if (provider.getAcceptedCareLevels() != null && !provider.getAcceptedCareLevels().isEmpty()) {
            if (provider.getAcceptedCareLevels().contains(patient.getCareLevel())) {
                return 1.0; // exact match
            }
            boolean adjacent = provider.getAcceptedCareLevels().stream()
                    .anyMatch(l -> Math.abs(l - patient.getCareLevel()) == 1);
            return adjacent ? 0.6 : 0.0;
        }
        // Fallback: assume all care levels supported (original behaviour)
        return 1.0;
    }

    private double calculateDistanceScore(PatientProfileDTO patient, ProviderProfileDTO provider) {
        if (patient.getLatitude() == null || patient.getLongitude() == null ||
                provider.getLatitude() == null || provider.getLongitude() == null) {
            return 0.5;
        }
        double distance = calculateDistance(
                patient.getLatitude(), patient.getLongitude(),
                provider.getLatitude(), provider.getLongitude());

        if (distance <= 10)  return 1.0;
        if (distance <= 25)  return 0.75;
        if (distance <= 50)  return 0.5;
        if (distance <= 100) return 0.25;
        return 0.0;
    }

    private double calculateSpecializationScore(PatientProfileDTO patient, ProviderProfileDTO provider) {
        if (patient.getMedicalRequirements() == null || patient.getMedicalRequirements().isEmpty() ||
                provider.getSpecializations() == null || provider.getSpecializations().isEmpty()) {
            return 0.5;
        }
        Set<String> patientNeeds = extractMedicalRequirements(patient.getMedicalRequirements());
        Set<String> providerSpecs = new HashSet<>();
        for (String spec : provider.getSpecializations()) {
            providerSpecs.add(spec.toLowerCase());
        }
        if (patientNeeds.isEmpty()) return 0.5;

        Set<String> matches = new HashSet<>(patientNeeds);
        matches.retainAll(providerSpecs);
        return (double) matches.size() / patientNeeds.size();
    }

    private double calculateLifestyleScore(PatientProfileDTO patient, ProviderProfileDTO provider) {
        if (patient.getLifestyleAttributes() == null || patient.getLifestyleAttributes().isEmpty()) {
            return 0.5;
        }
        if (provider.getLifestyleOptions() == null || provider.getLifestyleOptions().isEmpty()) {
            return 0.7; // base score — provider data not available
        }

        double score = 0.0;
        int checks = 0;

        // pets
        Object patientPets = patient.getLifestyleAttributes().get("petsAllowed");
        Object providerPets = provider.getLifestyleOptions().get("petsAllowed");
        if (patientPets != null && providerPets != null) {
            score += Boolean.parseBoolean(patientPets.toString()) ==
                    Boolean.parseBoolean(providerPets.toString()) ? 1.0 : 0.0;
            checks++;
        }

        // smoking
        Object patientSmoke = patient.getLifestyleAttributes().get("smokingAllowed");
        Object providerSmoke = provider.getLifestyleOptions().get("smokingAllowed");
        if (patientSmoke != null && providerSmoke != null) {
            score += Boolean.parseBoolean(patientSmoke.toString()) ==
                    Boolean.parseBoolean(providerSmoke.toString()) ? 1.0 : 0.0;
            checks++;
        }

        return checks == 0 ? 0.7 : score / checks;
    }

    private double calculateSocialScore(PatientProfileDTO patient, ProviderProfileDTO provider) {
        if (patient.getLifestyleAttributes() == null) {
            return 0.5;
        }
        // Base score — extended when richer provider social data is available
        return 0.6;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NEW: calculateTierScore — 15% weight
    //
    // Tier compatibility matrix (patient tier × best provider tier offered):
    //             STANDARD  COMFORT  PREMIUM
    // STANDARD      1.0      0.7      0.2
    // COMFORT        0.4      1.0      0.6
    // PREMIUM        0.0      0.5      1.0
    //
    // When provider offers multiple tiers, the best (highest) score is used.
    // ─────────────────────────────────────────────────────────────────────────
    private double calculateTierScore(PatientProfileDTO patient, ProviderProfileDTO provider) {
        String patientTier = patient.getCareServiceTier() != null
                ? patient.getCareServiceTier().toUpperCase() : "STANDARD";

        Set<String> providerTiers = provider.getOfferedServiceTiers();
        if (providerTiers == null || providerTiers.isEmpty()) {
            // Provider has no tier data yet — treat as STANDARD, score neutrally
            providerTiers = Set.of("STANDARD");
        }

        double bestScore = 0.0;
        for (String pt : providerTiers) {
            double s = tierCompatibilityScore(patientTier, pt.toUpperCase());
            if (s > bestScore) bestScore = s;
        }
        return bestScore;
    }

    private double tierCompatibilityScore(String patientTier, String providerTier) {
        return switch (patientTier) {
            case "STANDARD" -> switch (providerTier) {
                case "STANDARD" -> 1.0;
                case "COMFORT"  -> 0.7;
                case "PREMIUM"  -> 0.2;
                default         -> 0.5;
            };
            case "COMFORT" -> switch (providerTier) {
                case "STANDARD" -> 0.4;
                case "COMFORT"  -> 1.0;
                case "PREMIUM"  -> 0.6;
                default         -> 0.5;
            };
            case "PREMIUM" -> switch (providerTier) {
                case "STANDARD" -> 0.0;
                case "COMFORT"  -> 0.5;
                case "PREMIUM"  -> 1.0;
                default         -> 0.5;
            };
            default -> 0.5;
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NEW: calculateQualityScore — 5% weight
    //
    // Reads qualityIndicators map from the provider profile:
    //   "averageRating"  → Double 0–5, normalised to 0.0–1.0
    //   "certifications" → any non-blank value adds 0.1 bonus (capped at 1.0)
    // ─────────────────────────────────────────────────────────────────────────
    private double calculateQualityScore(PatientProfileDTO patient, ProviderProfileDTO provider) {
        if (provider.getQualityIndicators() == null || provider.getQualityIndicators().isEmpty()) {
            return 0.5; // neutral when no data available
        }

        double score = 0.5; // base

        Object ratingObj = provider.getQualityIndicators().get("averageRating");
        if (ratingObj != null) {
            try {
                double rating = Double.parseDouble(ratingObj.toString());
                score = Math.min(rating / 5.0, 1.0); // normalise 0–5 → 0.0–1.0
            } catch (NumberFormatException ignored) {}
        }

        Object certObj = provider.getQualityIndicators().get("certifications");
        if (certObj != null && !certObj.toString().isBlank()) {
            score = Math.min(score + 0.1, 1.0);
        }

        return score;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXISTING utility methods — unchanged
    // ─────────────────────────────────────────────────────────────────────────

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    private Set<String> extractMedicalRequirements(Map<String, Object> medicalRequirements) {
        Set<String> requirements = new HashSet<>();
        for (Map.Entry<String, Object> entry : medicalRequirements.entrySet()) {
            if (entry.getValue() instanceof Boolean && (Boolean) entry.getValue()) {
                requirements.add(entry.getKey().toLowerCase());
            }
        }
        return requirements;
    }

    private String generateSummary(BigDecimal score) {
        double v = score.doubleValue();
        if (v >= 90) return "Excellent match - highly recommended";
        if (v >= 75) return "Very good match - recommended";
        if (v >= 60) return "Good match - suitable";
        if (v >= 50) return "Moderate match - consider alternatives";
        return "Limited match - explore other options";
    }
}