package capstone_project.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.gemini")
@Data
public class GeminiConfig {
    private String apiKey;
    private String model = "gemini-2.0-flash-exp";
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private Double temperature = 0.7;
    private Integer maxTokens = 2048;
}
