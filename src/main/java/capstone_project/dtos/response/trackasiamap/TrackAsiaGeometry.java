package capstone_project.dtos.response.trackasiamap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackAsiaGeometry {
    private TrackAsiaLocation location;

    @JsonProperty("location_type")
    private String locationType;
}
