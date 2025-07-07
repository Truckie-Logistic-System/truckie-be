package capstone_project.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonHelper {

    private final ObjectMapper objectMapper;

    public JsonHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception var2) {
            return obj.toString();
        }
    }
}
