package capstone_project.dtos.response.trackasia.PlaceDetails;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TrackAsiaPlaceDetailsResponse {
    private String status;

    @JsonProperty("html_attributions")
    private List<String> htmlAttributions;

    private TrackAsiaPlaceDetailsResult result;
}

