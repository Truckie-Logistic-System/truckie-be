package capstone_project.dtos.response.trackasia.AutoComplete;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchedSubstring {
    public int length;
    public int offset;
}
