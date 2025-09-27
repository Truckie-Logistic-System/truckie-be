package capstone_project.dtos.response.trackasia.PlaceDetails;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@Data
@NoArgsConstructor
public class OpeningHours {
    @JsonProperty("open_now")
    private Boolean openNow;

    @JsonProperty("weekday_text")
    private List<String> weekdayText;
}
