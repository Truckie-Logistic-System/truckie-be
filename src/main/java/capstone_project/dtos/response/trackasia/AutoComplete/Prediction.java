package capstone_project.dtos.response.trackasia.AutoComplete;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Prediction {
    public String place_id;
    public String reference;
    public String name;
    public String description;
    public String formatted_address;
    public String icon;
    public List<MatchedSubstring> matched_substrings;
    public StructuredFormatting structured_formatting;
    public List<Term> terms;
    public List<String> types;
    public String old_description;
    public String old_formatted_address;
}
