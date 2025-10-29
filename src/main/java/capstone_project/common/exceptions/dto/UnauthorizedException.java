package capstone_project.common.exceptions.dto;

import capstone_project.common.enums.ErrorEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UnauthorizedException extends RuntimeException {

    private final String message;
    private final long errorCode;

    public UnauthorizedException(ErrorEnum errorEnum) {
        super(errorEnum.getMessage());
        this.message = errorEnum.getMessage();
        this.errorCode = errorEnum.getErrorCode();
    }
}

