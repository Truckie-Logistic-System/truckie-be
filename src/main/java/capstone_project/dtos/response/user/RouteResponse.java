package capstone_project.dtos.response.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RouteResponse(
        @JsonProperty("paths") List<Path> paths,
        @JsonProperty("code") String code,
        @JsonProperty("message") String message
) {
    public record Path(
            @JsonProperty("distance") double distance,
            @JsonProperty("time") long time,
            @JsonProperty("weight") double weight,
            @JsonProperty("points_encoded") boolean pointsEncoded,
            @JsonProperty("bbox") List<Double> bbox,
            @JsonProperty("instructions") List<Instruction> instructions
    ) {
        public record Instruction(
                @JsonProperty("distance") double distance,
                @JsonProperty("heading") int heading,
                @JsonProperty("sign") int sign,
                @JsonProperty("interval") List<Integer> interval,
                @JsonProperty("text") String text,
                @JsonProperty("time") long time,
                @JsonProperty("street_name") String streetName
        ) {}
    }
}