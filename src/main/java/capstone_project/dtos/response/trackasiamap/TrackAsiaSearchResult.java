package capstone_project.dtos.response.trackasiamap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackAsiaSearchResult {
    private String name;

    @JsonProperty("formatted_address")
    private String formattedAddress;

    @JsonProperty("address_components")
    private List<TrackAsiaAddressComponent> addressComponents;

    private List<String> types;

    private TrackAsiaGeometry geometry;
}
