package capstone_project.entity.fcm;

import capstone_project.entity.auth.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fcm_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FCMTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "device_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public enum DeviceType {
        ANDROID,
        IOS,
        WEB
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Mark token as inactive (when FCM returns UNREGISTERED)
     */
    public void markAsInactive() {
        this.isActive = false;
    }

    /**
     * Update last used timestamp
     */
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if token is valid (active and not expired)
     */
    public boolean isValid() {
        return isActive && !isExpired();
    }
}
