package capstone_project.entity.common;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@MappedSuperclass
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Serializable {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    @EqualsAndHashCode.Include
    private UUID id;

    // @CreatedDate - DISABLED to prevent override for demo data
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "modified_by")
    private String modifiedBy;

    /**
     * Flag to mark demo/seed data for testing dashboards
     * Used to easily clear test data without affecting real production data
     */
    @lombok.Builder.Default
    @Column(name = "is_demo_data")
    private Boolean isDemoData = false;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    
    @PrePersist
    public void prePersist() {
        // Set createdAt to now for all non-demo data
        // Demo data should have createdAt set manually and isDemoData=true to preserve the date
        // Always use Vietnam timezone (UTC+7) for consistency
        if (createdAt == null) {
            createdAt = LocalDateTime.now(VIETNAM_ZONE);
        }
        if (isDemoData == null) {
            isDemoData = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        modifiedAt = LocalDateTime.now(VIETNAM_ZONE);
    }


}
