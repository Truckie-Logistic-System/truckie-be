package capstone_project.service.services.thirdPartyServices.Province.impl;

import capstone_project.dtos.response.province.ProvinceResponse;
import capstone_project.service.services.thirdPartyServices.Province.ProvinceService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class ProvinceServiceImpl implements ProvinceService {
    private static final Logger LOGGER = Logger.getLogger(ProvinceServiceImpl.class.getName());
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    // simple in-memory cache
    private volatile List<ProvinceResponse> cache = Collections.emptyList();
    private volatile Instant cacheTime = Instant.EPOCH;
    private final Duration ttl = Duration.ofHours(24);

    public ProvinceServiceImpl(RestTemplateBuilder builder,
                                @Value("${province.api.base-url}") String baseUrl) {
        this.restTemplate = builder.build();
        this.baseUrl = baseUrl != null ? baseUrl : "";
    }

    @Override
    public List<ProvinceResponse> getAllProvinces() {
        Instant now = Instant.now();
        if (cache != null && !cache.isEmpty() && cacheTime.plus(ttl).isAfter(now)) {
            return cache;
        }
        synchronized (this) {
            now = Instant.now();
            if (cache != null && !cache.isEmpty() && cacheTime.plus(ttl).isAfter(now)) {
                return cache;
            }
            try {
                ResponseEntity<String> resp = restTemplate.getForEntity(baseUrl, String.class);
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    List<ProvinceResponse> list = mapper.readValue(resp.getBody(), new TypeReference<List<ProvinceResponse>>() {});
                    cache = list;
                    cacheTime = Instant.now();
                    return cache;
                } else {
                    LOGGER.log(Level.WARNING, "Third-party API returned non-OK: {0}", resp.getStatusCode());
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error fetching provinces from third-party API", e);
            }
            return (cache != null) ? cache : Collections.emptyList();
        }
    }

    // optional helper to force refresh
    public void refreshCache() {
        synchronized (this) {
            cacheTime = Instant.EPOCH;
            getAllProvinces();
        }
    }
}
