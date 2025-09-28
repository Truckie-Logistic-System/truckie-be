package capstone_project.dtos.response.trackasia.PlaceDetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackAsiaPlaceDetailsResponse {
    private String status;

    @JsonProperty("html_attributions")
    private List<Object> htmlAttributions;

    private TrackAsiaPlaceDetailsResult result;
}

