package capstone_project.common.enums;

import lombok.Getter;

@Getter
public enum ErrorEnum {
    USER_BY_ID_NOT_FOUND(1, "User by id {} is not found."),
    USER_NAME_OR_EMAIL_EXISTED(2, "Username or Email is existed."),
    INTERNAL_SERVER_ERROR(3, "Something went wrong with us. Please patience!"),
    LOGIN_NOT_FOUND_USER_NAME_OR_EMAIL(4, "Username and Email are not found."),
    LOGIN_WRONG_PASSWORD(5, "Wrong password."),
    INVALID_DATE_FORMAT(6, "Invalid date format. Expected format: yyyy-MM-dd."),
    ALREADY_EXISTED(9, "This is already exists"),
    NULL(13, "This field is null."),
    NOT_FOUND(15, "Not found");
    private final String message;
    private final long errorCode;

    ErrorEnum(final long errorCode, final String message) {
        this.errorCode = errorCode;
        this.message = message;
    }
}
