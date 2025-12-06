package capstone_project.service.services.ai;

import capstone_project.config.ai.GeminiConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final GeminiConfig geminiConfig;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();

    /**
     * Gọi Gemini API với conversation history using REST API with retry mechanism
     */
    public String generateResponse(String systemPrompt, List<ChatMessage> messages) {
        int maxRetries = 3;
        long[] retryDelays = {1000, 2000, 4000}; // Exponential backoff in milliseconds
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
            // Build request URL
            String url = String.format("%s/models/%s:generateContent",
                    geminiConfig.getBaseUrl(),
                    geminiConfig.getModel());

            // Build request body
            JsonObject requestBody = new JsonObject();
            
            // Add contents array
            JsonArray contents = new JsonArray();
            
            // Add system instruction as first user message if provided
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                JsonObject systemContent = new JsonObject();
                systemContent.addProperty("role", "user");
                JsonArray systemParts = new JsonArray();
                JsonObject systemPart = new JsonObject();
                systemPart.addProperty("text", systemPrompt);
                systemParts.add(systemPart);
                systemContent.add("parts", systemParts);
                contents.add(systemContent);
            }
            
            // Add conversation messages
            for (ChatMessage msg : messages) {
                JsonObject content = new JsonObject();
                
                // Convert role: "assistant" → "model" for Gemini
                String role = msg.getRole().equals("assistant") ? "model" : "user";
                content.addProperty("role", role);
                
                // Add parts
                JsonArray parts = new JsonArray();
                JsonObject part = new JsonObject();
                part.addProperty("text", msg.getContent());
                parts.add(part);
                content.add("parts", parts);
                
                contents.add(content);
            }
            
            requestBody.add("contents", contents);
            
            // Add generation config
            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("temperature", geminiConfig.getTemperature());
            generationConfig.addProperty("maxOutputTokens", geminiConfig.getMaxTokens());
            requestBody.add("generationConfig", generationConfig);

            // Build HTTP request
            RequestBody body = RequestBody.create(
                    gson.toJson(requestBody),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", geminiConfig.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            // Execute request
            log.debug("🤖 Calling Gemini API: {}", url);
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    log.error("❌ Gemini API error: {} - {}", response.code(), errorBody);
                    throw new RuntimeException("Gemini API call failed: " + response.code() + " - " + errorBody);
                }

                String responseBody = response.body().string();
                log.debug("📝 Raw response: {}", responseBody);

                // Parse response
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                
                // Extract text from first candidate
                if (jsonResponse.has("candidates") && jsonResponse.getAsJsonArray("candidates").size() > 0) {
                    JsonObject firstCandidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();
                    JsonObject content = firstCandidate.getAsJsonObject("content");
                    JsonArray responseParts = content.getAsJsonArray("parts");
                    
                    // Add null check to prevent NullPointerException
                    if (responseParts != null && responseParts.size() > 0) {
                        String result = responseParts.get(0).getAsJsonObject().get("text").getAsString();
                        log.debug("✅ Gemini response: {}", result.substring(0, Math.min(100, result.length())));
                        return result;
                    }
                }

                throw new RuntimeException("No valid response from Gemini API");
            }

        } catch (IOException e) {
            log.error("❌ IO Error calling Gemini API (attempt {}/{})", attempt + 1, maxRetries, e);
            if (attempt < maxRetries - 1) {
                try {
                    Thread.sleep(retryDelays[attempt]);
                    log.info("🔄 Retrying Gemini API call after {}ms...", retryDelays[attempt]);
                    continue;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
                }
            }
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Error calling Gemini API (attempt {}/{})", attempt + 1, maxRetries, e);
            if (attempt < maxRetries - 1) {
                try {
                    Thread.sleep(retryDelays[attempt]);
                    log.info("🔄 Retrying Gemini API call after {}ms...", retryDelays[attempt]);
                    continue;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
                }
            }
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
        }
        
        // If we get here, all retries failed
        throw new RuntimeException("Failed to call Gemini API after " + maxRetries + " attempts");
    }


    /**
     * ChatMessage class để lưu conversation history
     */
    public static class ChatMessage {
        private final String role; // "user" or "assistant"
        private final String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}
