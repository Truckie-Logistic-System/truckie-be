package capstone_project.service.ThirdPartyServices.TrackAsiaMap.impl;

import capstone_project.dtos.response.trackasiamap.TrackAsiaSearchResponse;
import capstone_project.service.ThirdPartyServices.TrackAsiaMap.TrackAsiaMapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class TrackAsiaMapServiceImpl implements TrackAsiaMapService {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String searchEndpoint;
    private final String apiKey;

    public TrackAsiaMapServiceImpl(RestTemplateBuilder builder,
                                   @Value("${trackasia.api.base-url}") String baseUrl,
                                   @Value("${trackasia.api.search.endpoint}") String searchEndpoint,
                                   @Value("${trackasia.api.key}") String apiKey) {
        this.restTemplate = builder.build();
        this.baseUrl = baseUrl != null ? baseUrl : "";
        this.searchEndpoint = searchEndpoint != null ? searchEndpoint : "/place/textsearch";
        this.apiKey = apiKey != null ? apiKey : "";
    }

    @Override
    public TrackAsiaSearchResponse search(String query) {
        try {
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String encodedQuery = URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8);
            log.info("Searching TrackAsiaMap for query: {}", query);
            log.info("baseUrl: {}", baseUrl);
            log.info("searchEndpoint: {}", searchEndpoint);
            log.info("Full URL: {}{}?language=en&key={}&query={}&new_admin=true",
                    trimSlash(baseUrl),
                    trimLeadingSlash(searchEndpoint),
                    encodedKey,
                    encodedQuery);
            String url = String.format("%s%s/json?language=en&key=%s&query=%s&new_admin=true",
                    trimSlash(baseUrl),
                    trimLeadingSlash(searchEndpoint),
                    encodedKey,
                    encodedQuery);

            ResponseEntity<TrackAsiaSearchResponse> resp = restTemplate.getForEntity(url, TrackAsiaSearchResponse.class);
            TrackAsiaSearchResponse body = resp.getBody();
            return body != null ? body : new TrackAsiaSearchResponse();
        } catch (Exception ex) {
            // swallow and return empty response so callers can handle absence
            return new TrackAsiaSearchResponse();
        }
    }

    private String trimSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private String trimLeadingSlash(String s) {
        if (s == null) return "";
        return s.startsWith("/") ? s : "/" + s;
    }
}
