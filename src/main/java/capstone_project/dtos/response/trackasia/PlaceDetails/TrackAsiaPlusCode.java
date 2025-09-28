package capstone_project.dtos.response.trackasia.PlaceDetails;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
public class TrackAsiaPlusCode {
    @JsonProperty("compound_code")
    private String compoundCode;

    @JsonProperty("global_code")
    private String globalCode;
}
