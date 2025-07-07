package capstone_project.controller.dtos.response;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

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
    private Date timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .statusCode(200)
                .data(data)
                .timestamp(new Date())
                .build();
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .statusCode(200)
                .data(data)
                .timestamp(new Date())
                .build();
    }

    public static <T> ApiResponse<T> fail(String message, int statusCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .statusCode(statusCode)
                .timestamp(new Date())
                .build();
    }
}
