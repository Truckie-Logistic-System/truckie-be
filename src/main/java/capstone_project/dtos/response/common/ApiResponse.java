package capstone_project.dtos.response.common;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = -7690643998118474L;

    private boolean success;
    private String message;
    private int statusCode;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .statusCode(200)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .statusCode(200)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> fail(String message, int statusCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .statusCode(statusCode)
                .build();
    }
}
