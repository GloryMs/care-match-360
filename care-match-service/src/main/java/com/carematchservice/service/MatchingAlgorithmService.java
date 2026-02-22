package com.carematchservice.service;

import com.carematchservice.dto.PatientProfileDTO;
import com.carematchservice.dto.ProviderProfileDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Slf4j
public class MatchingAlgorithmService {

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

    public BigDecimal calculateMatchScore(PatientProfileDTO patient, ProviderProfileDTO provider) {
        double totalScore = 0.0;
        Map<String, Double> breakdown = new HashMap<>();

        // 1. Care Level Compatibility (30%)
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

        // 4. Lifestyle Compatibility (20%)
        double lifestyleScore = calculateLifestyleScore(patient, provider);
        totalScore += lifestyleScore * lifestyleWeight;
        breakdown.put("lifestyle", lifestyleScore);

        // 5. Social Compatibility (10%)
        double socialScore = calculateSocialScore(patient, provider);
        totalScore += socialScore * socialWeight;
        breakdown.put("social", socialScore);

        log.debug("Match score calculated: patient={}, provider={}, score={}, breakdown={}",
                patient.getId(), provider.getId(), totalScore, breakdown);

        return BigDecimal.valueOf(totalScore).setScale(2, RoundingMode.HALF_UP);
    }

    public Map<String, Object> generateExplanation(
            PatientProfileDTO patient,
            ProviderProfileDTO provider,
            BigDecimal score) {

        Map<String, Object> explanation = new HashMap<>();
        List<String> reasons = new ArrayList<>();

        // Care Level
        if (patient.getCareLevel() != null) {
            reasons.add(String.format("Care level compatibility: Patient requires level %d care",
                    patient.getCareLevel()));
        }

        // Distance
        double distance = calculateDistance(
                patient.getLatitude(), patient.getLongitude(),
                provider.getLatitude(), provider.getLongitude()
        );
        reasons.add(String.format("Located %.1f km away", distance));

        // Specializations
        if (patient.getMedicalRequirements() != null && provider.getSpecializations() != null) {
            Set<String> patientNeeds = extractMedicalRequirements(patient.getMedicalRequirements());
            Set<String> providerSpecs = new HashSet<>(provider.getSpecializations());

            Set<String> matches = new HashSet<>(patientNeeds);
            matches.retainAll(providerSpecs);

            if (!matches.isEmpty()) {
                reasons.add("Specialized in: " + String.join(", ", matches));
            }
        }

        // Lifestyle
        if (patient.getLifestyleAttributes() != null) {
            reasons.add("Lifestyle preferences considered");
        }

        explanation.put("score", score);
        explanation.put("reasons", reasons);
        explanation.put("summary", generateSummary(score));

        return explanation;
    }

    private double calculateCareLevelScore(PatientProfileDTO patient, ProviderProfileDTO provider) {
        if (patient.getCareLevel() == null) {
            return 0.5; // Neutral score if no care level specified
        }

        // Provider should meet or exceed patient's care level
        // For simplicity, assume all providers can handle all care levels
        // In reality, you'd check provider capabilities
        return 1.0;
    }

    private double calculateDistanceScore(PatientProfileDTO patient, ProviderProfileDTO provider) {
        if (patient.getLatitude() == null || patient.getLongitude() == null ||
                provider.getLatitude() == null || provider.getLongitude() == null) {
            return 0.5; // Neutral score if location not available
        }

        double distance = calculateDistance(
                patient.getLatitude(), patient.getLongitude(),
                provider.getLatitude(), provider.getLongitude()
        );

        // Scoring based on distance (km)
        if (distance <= 10) {
            return 1.0;
        } else if (distance <= 25) {
            return 0.75;
        } else if (distance <= 50) {
            return 0.5;
        } else if (distance <= 100) {
            return 0.25;
        } else {
            return 0.0;
        }
    }

    private double calculateSpecializationScore(PatientProfileDTO patient, ProviderProfileDTO provider) {
        if (patient.getMedicalRequirements() == null || patient.getMedicalRequirements().isEmpty() ||
                provider.getSpecializations() == null || provider.getSpecializations().isEmpty()) {
            return 0.5; // Neutral score
        }

        Set<String> patientNeeds = extractMedicalRequirements(patient.getMedicalRequirements());
        Set<String> providerSpecs = new HashSet<>();
        for (String spec : provider.getSpecializations()) {
            providerSpecs.add(spec.toLowerCase());
        }

        if (patientNeeds.isEmpty()) {
            return 0.5;
        }

        // Calculate overlap
        Set<String> matches = new HashSet<>(patientNeeds);
        matches.retainAll(providerSpecs);

        double matchRatio = (double) matches.size() / patientNeeds.size();
        return matchRatio;
    }

    private double calculateLifestyleScore(PatientProfileDTO patient, ProviderProfileDTO provider) {
        if (patient.getLifestyleAttributes() == null || patient.getLifestyleAttributes().isEmpty()) {
            return 0.5;
        }

        // Simplified lifestyle matching
        // In a real system, you'd have more sophisticated matching logic
        double score = 0.7; // Base score

        // Example: Check diet preferences, language, etc.
        // This would require more detailed provider profile information

        return score;
    }

    private double calculateSocialScore(PatientProfileDTO patient, ProviderProfileDTO provider) {
        if (patient.getLifestyleAttributes() == null) {
            return 0.5;
        }

        // Simplified social compatibility
        // Consider: social interaction preferences, group size preferences, etc.
        double score = 0.6; // Base score

        return score;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula for distance calculation
        final int EARTH_RADIUS = 6371; // Radius in kilometers

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
        double scoreValue = score.doubleValue();

        if (scoreValue >= 90) {
            return "Excellent match - highly recommended";
        } else if (scoreValue >= 75) {
            return "Very good match - recommended";
        } else if (scoreValue >= 60) {
            return "Good match - suitable";
        } else if (scoreValue >= 50) {
            return "Moderate match - consider alternatives";
        } else {
            return "Limited match - explore other options";
        }
    }

    public Map<String, Object> getScoreBreakdown(PatientProfileDTO patient, ProviderProfileDTO provider) {
        Map<String, Object> breakdown = new HashMap<>();

        breakdown.put("careLevelScore", calculateCareLevelScore(patient, provider));
        breakdown.put("distanceScore", calculateDistanceScore(patient, provider));
        breakdown.put("specializationScore", calculateSpecializationScore(patient, provider));
        breakdown.put("lifestyleScore", calculateLifestyleScore(patient, provider));
        breakdown.put("socialScore", calculateSocialScore(patient, provider));

        breakdown.put("weights", Map.of(
                "careLevel", careLevelWeight,
                "distance", distanceWeight,
                "specialization", specializationWeight,
                "lifestyle", lifestyleWeight,
                "social", socialWeight
        ));

        return breakdown;
    }
}
