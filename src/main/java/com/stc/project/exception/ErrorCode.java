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
    PASSWORD_OR_EMAIL_USERNAME_INCORRECT("Password or email/username incorrect", HttpStatus.UNAUTHORIZED);
    String errorMessage;
    HttpStatus status;

}
