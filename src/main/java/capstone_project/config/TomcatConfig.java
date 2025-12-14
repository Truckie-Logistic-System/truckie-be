package capstone_project.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            // Create custom temp directory
            String tempDirPath = System.getProperty("user.dir") + File.separator + "tomcat-temp";
            File tempDir = new File(tempDirPath);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            
            // Set custom temp directory
            factory.setBaseDirectory(tempDir);
            
            // Disable access log
            factory.getEngineValves().clear();
        };
    }
}
