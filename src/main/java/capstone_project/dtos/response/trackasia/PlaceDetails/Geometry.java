package capstone_project.dtos.response.trackasia.PlaceDetails;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
public class Geometry {
    private Location location;

    @JsonProperty("location_type")
    private String locationType;

    private Viewport viewport;

    // Optional: keep the raw shape (GeoJSON-like) if the API returns it
    @JsonProperty("shape")
    private JsonNode shape;
}
