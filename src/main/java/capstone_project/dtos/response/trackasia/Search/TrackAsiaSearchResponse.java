package capstone_project.dtos.response.trackasia.Search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackAsiaSearchResponse {
    private String status;
    private List<TrackAsiaSearchResult> results = new ArrayList<>();
}
