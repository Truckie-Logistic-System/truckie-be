package capstone_project.dtos.response.trackasia.PlaceDetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackAsiaPlaceDetailsResult {
    @JsonProperty("address_components")
    private List<TrackAsiaAddressComponent> addressComponents;

    @JsonProperty("adr_address")
    private String adrAddress;

    @JsonProperty("formatted_address")
    private String formattedAddress;

    private Geometry geometry;

    private String icon;

    @JsonProperty("icon_background_color")
    private String iconBackgroundColor;

    private String name;

    @JsonProperty("place_id")
    private String placeId;

    @JsonProperty("plus_code")
    private TrackAsiaPlusCode plusCode;

    private List<String> socials;

    private List<String> types;

    private String url;

    @JsonProperty("utc_offset")
    private Integer utcOffset;

    private String vicinity;

    private String website;

    // map JSON "class" to a safe Java name
    @JsonProperty("class")
    private String clazz;

    private String subclass;
}
