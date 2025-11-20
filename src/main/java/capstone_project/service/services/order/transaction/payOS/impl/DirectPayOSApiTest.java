package capstone_project.service.services.order.transaction.payOS.impl;

import lombok.extern.slf4j.Slf4j;
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
 * Direct PayOS API call to bypass SDK and test signature verification
 * This helps identify if issue is with SDK or PayOS server
 */
@Slf4j
@Component
public class DirectPayOSApiTest {

    private static final String CLIENT_ID = "2ab830e8-969b-49d8-a004-509cd41bacde";
    private static final String API_KEY = "def546f5-b8ce-44f4-9d18-689d3f8f1776";
    private static final String CHECKSUM_KEY = "a5ae70ddf44aef3e4b3891131e67c66e4f07ad2c94ea743081e90837da723c8d";
    private static final String BASE_URL = "https://api-merchant.payos.vn";

    public void testDirectPayOSCall() {

        try {
            long orderCode = System.currentTimeMillis();
            
            // Create request body
            String requestBody = String.format("""
                {
                  "orderCode": %d,
                  "amount": 2000,
                  "description": "Test payment",
                  "returnUrl": "http://localhost:5173/payment/return",
                  "cancelUrl": "http://localhost:5173/payment/return"
                }
                """, orderCode);

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

            if (response.statusCode() == 200) {

            } else {
                log.error("❌ DIRECT API CALL FAILED!");
                log.error("❌ Status: {}", response.statusCode());
                log.error("❌ This means PayOS server-side issue or credentials problem");
            }
            
            // Try to verify signature manually
            String responseSignature = response.headers().firstValue("x-signature").orElse(null);
            if (responseSignature != null) {

                String calculatedSignature = calculateSignature(response.body(), CHECKSUM_KEY);

                if (responseSignature.equals(calculatedSignature)) {

                } else {
                    log.error("❌ MANUAL SIGNATURE VERIFICATION FAILED!");
                    log.error("❌ PayOS server signature generation is problematic");
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Error in direct PayOS API test", e);
        }

    }
    
    private String calculateSignature(String data, String key) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Error calculating signature", e);
            return null;
        }
    }
}
