package capstone_project.config.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

// DISABLED: Firebase/Firestore not in use
//@Configuration
//public class FirestoreConfig {
//    @Value("classpath:/private-key.json")
//    private Resource privateKey;
//
//    @Bean
//    public Firestore firestore() throws IOException {
//        InputStream credentials = new ByteArrayInputStream(privateKey.getContentAsByteArray());
//        GoogleCredentials googleCredentials = GoogleCredentials.fromStream(credentials);
//
//        FirebaseOptions firebaseOptions = FirebaseOptions.builder()
//                .setCredentials(googleCredentials)
//                .build();
//
//        if (FirebaseApp.getApps().isEmpty()) {
//            FirebaseApp.initializeApp(firebaseOptions);
//        }
//
//        return FirestoreClient.getFirestore();
//    }
//}
