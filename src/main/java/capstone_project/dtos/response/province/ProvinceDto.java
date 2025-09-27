package capstone_project.dtos.response.province;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ProvinceDto {
    private String name;
    private Integer code;

    @JsonProperty("division_type")
    private String divisionType;

    @JsonProperty("phone_code")
    private Integer phoneCode;

    private String codename;

    private List<DistrictDto> districts;
}
