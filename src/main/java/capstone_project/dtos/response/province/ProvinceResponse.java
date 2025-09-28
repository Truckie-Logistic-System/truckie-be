package capstone_project.dtos.response.province;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ProvinceResponse {
    private String name;
    private Integer code;

    @JsonProperty("division_type")
    private String divisionType;

    @JsonProperty("phone_code")
    private Integer phoneCode;

    private String codename;

    @JsonProperty("wards")
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<WardResponse> wards;
}
