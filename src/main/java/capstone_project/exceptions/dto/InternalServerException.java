package capstone_project.exceptions.dto;

import capstone_project.enums.ErrorEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InternalServerException extends RuntimeException {

    private final String message;
    private final long errorCode;

    public InternalServerException(ErrorEnum errorEnum) {
        super(errorEnum.getMessage());
        this.message = errorEnum.getMessage();
        this.errorCode = errorEnum.getErrorCode();
    }

    // Getters for message and code
}

