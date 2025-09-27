package capstone_project.dtos.response.trackasia.AutoComplete;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StructuredFormatting {
    public String main_text;
    public List<MatchedSubstring> main_text_matched_substrings;
    public String secondary_text;
}
