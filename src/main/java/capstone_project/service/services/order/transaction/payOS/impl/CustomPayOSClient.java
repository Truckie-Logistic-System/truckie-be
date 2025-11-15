package capstone_project.service.services.order.transaction.payOS.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Custom PayOS HTTP client that bypasses the buggy official SDK
 * This directly calls PayOS API without signature verification issues
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomPayOSClient {

    private final ObjectMapper objectMapper;
    
    private static final String CLIENT_ID = "2ab830e8-969b-49d8-a004-509cd41bacde";
    @Value("${payos.api-key}")
    private String API_KEY;
    
    @Value("${payos.checksum-key}")
    private String CHECKSUM_KEY;
    private static final String BASE_URL = "https://api-merchant.payos.vn";

    /**
     * Create payment link using direct HTTP call
     * Bypasses PayOS SDK to avoid signature verification bug
     */
    public PayOSResponse createPaymentLink(
            long orderCode,
            int amount,
            String description,
            String returnUrl,
            String cancelUrl
    ) throws Exception {
        
        log.info("🚀 Custom PayOS Client - Creating payment link (bypassing SDK)");
        log.info("   orderCode: {}", orderCode);
        log.info("   amount: {}", amount);
        log.info("   description: {}", description);
        
        // Generate signature as per PayOS docs
        String signatureData = String.format(
            "amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
            amount, cancelUrl, description, orderCode, returnUrl
        );
        String signature = generateHmacSHA256(signatureData, CHECKSUM_KEY);
        log.info("🔐 Generated signature: {}", signature);
        
        // Create request body with items and signature
        String requestBody = String.format("""
            {
              "orderCode": %d,
              "amount": %d,
              "description": "%s",
              "items": [
                {
                  "name": "Payment",
                  "quantity": 1,
                  "price": %d
                }
              ],
              "returnUrl": "%s",
              "cancelUrl": "%s",
              "signature": "%s"
            }
            """, orderCode, amount, description, amount, returnUrl, cancelUrl, signature);
        
        log.debug("📤 Request Body: {}", requestBody);
        
        // Create HTTP client
        HttpClient client = HttpClient.newHttpClient();
        
        // Build request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/v2/payment-requests"))
                .header("Content-Type", "application/json")
                .header("x-client-id", CLIENT_ID)
                .header("x-api-key", API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        
        // Send request
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        log.info("📥 Response Status: {}", response.statusCode());
        log.info("📥 Response Body (RAW): {}", response.body());
        
        if (response.statusCode() != 200) {
            log.error("❌ PayOS API Error: Status {}, Body: {}", response.statusCode(), response.body());
            throw new RuntimeException("PayOS API returned error: " + response.body());
        }
        
        // Parse response
        JsonNode responseJson = objectMapper.readTree(response.body());
        log.info("📄 Full Response JSON: {}", responseJson.toPrettyString());
        
        JsonNode data = responseJson.get("data");
        
        if (data == null) {
            log.error("❌ Response JSON: {}", responseJson.toPrettyString());
            throw new RuntimeException("Invalid PayOS response: missing 'data' field");
        }
        
        log.debug("📦 Data node: {}", data.toPrettyString());
        
        // Handle null-safe field extraction
        JsonNode checkoutUrlNode = data.get("checkoutUrl");
        JsonNode paymentLinkIdNode = data.get("paymentLinkId");
        
        if (checkoutUrlNode == null || paymentLinkIdNode == null) {
            log.error("❌ Missing fields in data. Available fields: {}", data.fieldNames());
            log.error("❌ Data content: {}", data.toPrettyString());
            throw new RuntimeException("Missing checkoutUrl or paymentLinkId in PayOS response");
        }
        
        String checkoutUrl = checkoutUrlNode.asText();
        String paymentLinkId = paymentLinkIdNode.asText();
        
        log.info("✅ Payment link created successfully!");
        log.info("   checkoutUrl: {}", checkoutUrl);
        log.info("   paymentLinkId: {}", paymentLinkId);
        
        return new PayOSResponse(
                checkoutUrl,
                paymentLinkId,
                orderCode,
                amount,
                "PENDING"
        );
    }
    
    /**
     * Generate HMAC SHA256 signature
     */
    private String generateHmacSHA256(String data, String key) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);
        byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
    
    /**
     * Simple response object for payment link creation
     */
    public record PayOSResponse(
            String checkoutUrl,
            String paymentLinkId,
            long orderCode,
            int amount,
            String status
    ) {}
}
