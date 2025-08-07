package capstone_project.service.services.cloudinary.Impl;

import capstone_project.service.services.cloudinary.CloudinaryService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public Map<String, Object> uploadFile(byte[] file, String fileName, String folder) throws IOException {
        try {
            Map<String, Object> params = ObjectUtils.asMap(
                    "public_id", fileName,
                    "folder", folder,
                    "resource_type", "auto" // This will automatically detect the file type
            );

            return cloudinary.uploader().upload(file, params);
        } catch (Exception e) {
            log.error("Error uploading file to Cloudinary: {}", e.getMessage(), e);
            throw new IOException("Failed to upload file to Cloudinary", e);
        }
    }


    @Override
    public String getFileUrl(String publicId) {
        return cloudinary.url().secure(true).generate(publicId);
    }
}