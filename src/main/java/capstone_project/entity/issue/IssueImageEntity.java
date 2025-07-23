package capstone_project.entity.issue;

import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "issue_images", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class IssueImageEntity extends BaseEntity {
    @Column(name = "image_url", length = Integer.MAX_VALUE)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private IssueEntity issueEntity;

}