package capstone_project.dtos.response.province;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class DistrictResponse {
    private String name;
    private Integer code;
    private String codename;

    @JsonProperty("division_type")
    private String divisionType;

    @JsonProperty("province_code")
    private Integer provinceCode;

    private List<WardResponse> wards;
}
