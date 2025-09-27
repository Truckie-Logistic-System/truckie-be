package capstone_project.dtos.response.trackasia.AutoComplete;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackAsiaAutocompleteResponse {
    public String status;
    public String warning_message;
    public List<Prediction> predictions;
}
