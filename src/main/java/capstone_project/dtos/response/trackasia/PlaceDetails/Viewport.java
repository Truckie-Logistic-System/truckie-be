package capstone_project.dtos.response.trackasia.PlaceDetails;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Viewport {
    private Location northeast;
    private Location southwest;
}
