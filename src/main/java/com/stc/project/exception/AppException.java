package com.stc.project.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;


@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
/**
 * @author Vanhle
 * Class này để quản lý các exception trong quá trình chạy
 * Có thể đúng về mặt technique nhưng sai logic nghiệp vụ
 */
public class AppException extends RuntimeException {

    ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getErrorMessage());
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }

}
