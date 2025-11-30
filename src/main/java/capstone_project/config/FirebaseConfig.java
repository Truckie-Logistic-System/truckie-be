package capstone_project.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${fcm.service.account.key}")
    private String serviceAccountKeyPath;

    @PostConstruct
    public void initializeFirebase() {
        try {
            // Load service account file
            InputStream serviceAccount = new ClassPathResource(
                serviceAccountKeyPath.replace("classpath:", "")
            ).getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

            // Initialize Firebase app if not already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase application initialized successfully");
            } else {
                log.info("Firebase application already initialized");
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage(), e);
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}
