package capstone_project.controller.trackasiamap;

import capstone_project.dtos.response.trackasiamap.TrackAsiaSearchResponse;
import capstone_project.service.ThirdPartyServices.TrackAsiaMap.TrackAsiaMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${trackasia.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TrackAsiaMapController {

    private final TrackAsiaMapService trackAsiaMapService;

    @GetMapping("/search")
    public ResponseEntity<TrackAsiaSearchResponse> search(@RequestParam("query") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        TrackAsiaSearchResponse response = trackAsiaMapService.search(query.trim());
        return ResponseEntity.ok(response);
    }
}
