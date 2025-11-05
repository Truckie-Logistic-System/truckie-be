package capstone_project.service.services.cloudinary;

import com.cloudinary.Cloudinary;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface CloudinaryService {

    Map<String, Object> uploadFile(byte[] file, String fileName, String folder) throws IOException;

    String getFileUrl(String publicId);
    
    String getRawFileUrl(String publicId);
}
