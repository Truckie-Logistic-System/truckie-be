package capstone_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

public class CustomApplication {
    
    public static void main(String[] args) {
        // Set custom temp directory before Spring Boot starts
        String tempDirPath = System.getProperty("user.dir") + File.separator + "tomcat-temp";
        File tempDir = new File(tempDirPath);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        
        // Set system property for temporary directory
        System.setProperty("java.io.tmpdir", tempDir.getAbsolutePath());
        
        // Start Spring Boot application
        SpringApplication.run(CapstoneProjectApplication.class, args);
    }
}
