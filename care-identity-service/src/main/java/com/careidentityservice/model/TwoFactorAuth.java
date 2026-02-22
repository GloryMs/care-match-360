package com.careidentityservice.model;

import com.carecommon.model.BaseEntity;
import com.careidentityservice.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "two_factor_auth", schema = "care_identity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorAuth extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "secret", nullable = false, length = 255)
    private String secret;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = false;

    @Column(name = "backup_codes", columnDefinition = "text[]")
    private List<String> backupCodes;
}
