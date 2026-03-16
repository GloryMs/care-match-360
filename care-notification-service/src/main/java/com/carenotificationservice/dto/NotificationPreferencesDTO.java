package com.carenotificationservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferencesDTO {

    private Boolean emailEnabled;
    private Boolean inAppEnabled;
    private Boolean matchAlerts;
    private Boolean offerAlerts;
    private Boolean systemAlerts;
}