package capstone_project.dtos.response.trackasia.PlaceDetails;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@Data
@NoArgsConstructor
public class Photo {
    @JsonProperty("photo_reference")
    private String photoReference;

    private Integer height;
    private Integer width;

    @JsonProperty("html_attributions")
    private List<String> htmlAttributions;

    // optional client-side helper
    private String url;
}
