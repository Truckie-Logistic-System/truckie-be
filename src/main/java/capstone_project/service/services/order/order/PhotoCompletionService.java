package capstone_project.service.services.order.order;

import capstone_project.dtos.request.order.CreatePhotoCompletionRequest;
import capstone_project.dtos.request.order.UpdatePhotoCompletionRequest;
import capstone_project.dtos.response.order.PhotoCompletionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface PhotoCompletionService {

    PhotoCompletionResponse uploadAndSavePhoto(MultipartFile file,
                                               CreatePhotoCompletionRequest request) throws IOException;

    PhotoCompletionResponse updatePhoto(UpdatePhotoCompletionRequest request);


    PhotoCompletionResponse getPhoto(UUID id);

    List<PhotoCompletionResponse> getAllPhotos();

    List<PhotoCompletionResponse> getByVehicleAssignment(UUID vehicleAssignmentId);
}
