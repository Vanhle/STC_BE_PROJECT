package com.stc.project.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

/**
 * @author Vanhle
 * Class này để mọi người định nghĩa ra các lỗi trong quá trình chạy
 * Có thể đúng về mặt technique nhưng sai logic nghiệp vụ
 * **/

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Getter
public enum ErrorCode {
    //Ví dụ khi tạo user mà đã tồn tại trong database thì sẽ bắn ra thong bao user existed
    USER_EXISTED("User existed", HttpStatus.BAD_REQUEST),
    INVALID_TOKEN("Invalid token", HttpStatus.UNAUTHORIZED),
    UNAUTHENTICATED("Unauthenticated", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("Token expired", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED("Account locked", HttpStatus.UNAUTHORIZED),
    PASSWORD_OR_EMAIL_USERNAME_INCORRECT("Password or email/username incorrect", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("Invalid refresh token", HttpStatus.UNAUTHORIZED),
    PASSWORD_AND_CONFIRM_PASSWORD_NOT_MATCH("Password and confirm password not match", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED("Email existed", HttpStatus.BAD_REQUEST),
    USER_ALREADY_VERIFIED("User already verified", HttpStatus.BAD_REQUEST),
    OTP_LOCKED("OTP Locked try again later", HttpStatus.BAD_REQUEST),
    EMAIL_OR_USERNAME_INCORRECT("Email or username incorrect", HttpStatus.BAD_REQUEST),
    INVALID_OTP("OTP không hợp lệ", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED("OTP đã hết hạn", HttpStatus.BAD_REQUEST),
    OTP_ALREADY_USED("OTP đã được sử dụng", HttpStatus.BAD_REQUEST),
    NEED_TO_VERIFY("Need to verify", HttpStatus.UNAUTHORIZED);
    String errorMessage;
    HttpStatus status;
}
