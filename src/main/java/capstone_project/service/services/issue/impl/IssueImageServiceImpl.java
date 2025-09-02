package capstone_project.service.services.issue.impl;

import capstone_project.dtos.request.issue.CreateIssueImageRequest;
import capstone_project.dtos.response.issue.GetIssueImageResponse;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.issue.IssueImageEntity;
import capstone_project.service.entityServices.issue.IssueEntityService;
import capstone_project.service.entityServices.issue.IssueImageEntityService;
import capstone_project.service.mapper.issue.IssueMapper;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.service.services.issue.IssueImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueImageServiceImpl implements IssueImageService {
    private final IssueMapper issueMapper;
    private final IssueEntityService issueEntityService;
    private final IssueImageEntityService issueImageEntityService;
    private final CloudinaryService cloudinaryService;

    @Override
    public GetIssueImageResponse createImage(CreateIssueImageRequest request) {
        IssueEntity issue = issueEntityService.findEntityById(request.issueId())
                .orElseThrow(() -> new RuntimeException("Issue not found with id " + request.issueId()));

        List<IssueImageEntity> images = new ArrayList<>();

        for (String base64Image : request.imageUrl()) {
            try {
                // Convert base64 -> byte[]
                byte[] fileBytes = Base64.getDecoder().decode(base64Image);

                // Generate unique filename
                String fileName = "issue_" + issue.getId() + "_" + UUID.randomUUID();

                // Upload lên Cloudinary (folder "issues")
                Map<String, Object> uploadResult = cloudinaryService.uploadFile(fileBytes, fileName, "issues");

                // Lấy URL trả về
                String imageUrl = (String) uploadResult.get("secure_url");

                // Lưu DB
                IssueImageEntity imageEntity = IssueImageEntity.builder()
                        .issueEntity(issue)
                        .imageUrl(imageUrl)
                        .build();

                images.add(imageEntity);

            } catch (Exception e) {
                log.error("Error uploading image for issue {}: {}", issue.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to upload image to Cloudinary", e);
            }
        }

        issueImageEntityService.saveAll(images);

        return new GetIssueImageResponse(issue, images);
    }

    @Override
    public GetIssueImageResponse getImage(UUID issueId) {
        List<IssueImageEntity> images = issueImageEntityService.findByIssueEntity_Id(issueId);
        IssueEntity issue = issueEntityService.findEntityById(issueId).get();
        return new GetIssueImageResponse(issue,images);
    }
}
