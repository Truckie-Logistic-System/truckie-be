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
            // Remove any existing extension from filename to prevent double extension
            String cleanFileName = fileName;
            int lastDotIndex = fileName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                cleanFileName = fileName.substring(0, lastDotIndex);
                
            }
            
            // Determine resource type based on original file extension
            String resourceType = "auto";
            String format = null;
            
            if (fileName.toLowerCase().endsWith(".pdf")) {
                resourceType = "raw"; // Use 'raw' for PDFs to prevent conversion
                format = "pdf";
                
            } else if (fileName.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|webp|bmp)$")) {
                resourceType = "image";
                
            } else {
                
            }

            Map<String, Object> params = ObjectUtils.asMap(
                    "public_id", cleanFileName,
                    "folder", folder,
                    "resource_type", resourceType,
                    "use_filename", true,
                    "unique_filename", false,
                    "overwrite", true
            );
            
            // Add format for PDFs to ensure proper handling
            if (format != null) {
                params.put("format", format);
            }
            // For images, don't set format parameter to prevent Cloudinary from auto-adding extensions

            Map<String, Object> result = cloudinary.uploader().upload(file, params);
            
            return result;
        } catch (Exception e) {
            log.error("Error uploading file {} to Cloudinary: {}", fileName, e.getMessage(), e);
            throw new IOException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
        }
    }

    @Override
    public String getFileUrl(String publicId) {
        return cloudinary.url().secure(true).generate(publicId);
    }
    
    @Override
    public String getRawFileUrl(String publicId) {
        // For raw files (PDFs, etc.), we need to specify resource_type as 'raw'
        // Add fl_attachment to force download and prevent Cloudinary from converting PDF to image preview
        return cloudinary.url()
                .secure(true)
                .resourceType("raw")
                .transformation(new com.cloudinary.Transformation().flags("attachment"))
                .generate(publicId);
    }
}