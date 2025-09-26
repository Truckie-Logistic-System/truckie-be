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
public class TrackAsiaAddressComponent {
    @JsonProperty("short_name")
    private String shortName;

    @JsonProperty("long_name")
    private String longName;

    private List<String> types;
}