package com.carenotificationservice.controller;

import com.carecommon.dto.ApiResponse;
import com.carenotificationservice.dto.NotificationPreferencesDTO;
import com.carenotificationservice.service.NotificationPreferencesService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
//@Tag(name = "Notification Preferences", description = "Manage per-profile notification preferences")
public class NotificationPreferencesController {

    private final NotificationPreferencesService preferencesService;

    @GetMapping("/{profileId}")
    //@Operation(summary = "Get notification preferences for a profile")
    public ResponseEntity<ApiResponse<NotificationPreferencesDTO>> getPreferences(
            @PathVariable UUID profileId) {

        NotificationPreferencesDTO prefs = preferencesService.getPreferences(profileId);
        return ResponseEntity.ok(ApiResponse.success(prefs));
    }

    @PutMapping("/{profileId}")
    //@Operation(summary = "Update notification preferences for a profile")
    public ResponseEntity<ApiResponse<NotificationPreferencesDTO>> updatePreferences(
            @PathVariable UUID profileId,
            @RequestBody NotificationPreferencesDTO dto) {

        NotificationPreferencesDTO updated = preferencesService.updatePreferences(profileId, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Notification preferences updated"));
    }
}