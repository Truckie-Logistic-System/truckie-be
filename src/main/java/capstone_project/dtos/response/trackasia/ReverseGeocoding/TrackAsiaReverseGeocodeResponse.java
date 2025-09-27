package capstone_project.dtos.response.trackasia.ReverseGeocoding;

import capstone_project.dtos.response.trackasia.Search.TrackAsiaSearchResult;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TrackAsiaReverseGeocodeResponse {
    private Object plus_code;
    private String status;
    private List<TrackAsiaSearchResult> results;
}
