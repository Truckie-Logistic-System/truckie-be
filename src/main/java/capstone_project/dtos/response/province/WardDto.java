package capstone_project.dtos.response.province;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WardDto {
    private String name;
    private Integer code;
    private String codename;

    @JsonProperty("division_type")
    private String divisionType;

    @JsonProperty("district_code")
    private Integer districtCode;
}

