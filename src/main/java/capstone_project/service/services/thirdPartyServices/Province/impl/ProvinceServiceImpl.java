package capstone_project.service.services.thirdPartyServices.Province.impl;

import capstone_project.dtos.response.province.ProvinceResponse;
import capstone_project.service.services.thirdPartyServices.Province.ProvinceService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
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
    private final ObjectMapper mapper;

    private volatile List<ProvinceResponse> cache = Collections.emptyList();
    private volatile Instant cacheTime = Instant.EPOCH;
    private final Duration ttl = Duration.ofHours(24);

    public ProvinceServiceImpl(RestTemplateBuilder builder,
                               @Value("${province.api.base-url}") String baseUrl,
                               ObjectMapper springMapper) {
        this.restTemplate = builder.build();
        this.baseUrl = (baseUrl != null) ? baseUrl : "";
        this.mapper = springMapper.copy()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.mapper.configOverride(List.class)
                .setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));
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
                String url = buildUrlWithDepth(baseUrl, 2); // depth=1 for province + wards
                ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);

                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    String body = resp.getBody();
                    List<ProvinceResponse> list = mapper.readValue(body, new com.fasterxml.jackson.core.type.TypeReference<List<ProvinceResponse>>() {});
                    cache = list;
                    cacheTime = Instant.now();
                    return cache;
                } else {
                    LOGGER.log(Level.WARNING, "Third-party API returned non-OK: {0}", resp.getStatusCode());
                    LOGGER.log(Level.WARNING, "Response body: {0}", resp.getBody());
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error fetching provinces from third-party API", e);
            }
            return (cache != null) ? cache : Collections.emptyList();
        }
    }

    private String buildUrlWithDepth(String base, int depth) {
        if (base == null || base.isEmpty()) return base;
        String param = "depth=" + depth;
        if (base.contains("?")) {
            if (base.endsWith("?") || base.endsWith("&")) return base + param;
            return base + "&" + param;
        } else {
            return base + "?" + param;
        }
    }

    public void refreshCache() {
        synchronized (this) {
            cacheTime = Instant.EPOCH;
            getAllProvinces();
        }
    }
}
