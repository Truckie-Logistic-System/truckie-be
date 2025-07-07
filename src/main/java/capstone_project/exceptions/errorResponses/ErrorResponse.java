package capstone_project.exceptions.errorResponses;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {
    private String message;
    private long errorCode;

    // Constructor to initialize fields
    public ErrorResponse(String message, long errorCode) {
        this.message = message;
        this.errorCode = errorCode;
    }
}
