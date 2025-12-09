package capstone_project.repository;

import capstone_project.entity.issue.IssueCompensationAssessmentEntity;
import capstone_project.entity.issue.IssueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueCompensationAssessmentRepository extends JpaRepository<IssueCompensationAssessmentEntity, UUID> {
    
    Optional<IssueCompensationAssessmentEntity> findByIssue(IssueEntity issue);
    
    Optional<IssueCompensationAssessmentEntity> findByIssueId(UUID issueId);
    
    Optional<IssueCompensationAssessmentEntity> findByIssueIdAndIssueType(UUID issueId, String issueType);
}
