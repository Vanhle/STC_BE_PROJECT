package com.stc.project.constants;

public class Constants {

    public static final String JWT_SECRET = "secret";
    public static final String AUTH_TOKEN_PREFIX = "Bearer ";
    public static final String AUTH_HEADER_STRING = "Authorization";
    public static final String JWT_SCOPE = "scope";

    //tiếng việt có dấu thì cần convert thành byte để tìm chính xác
    public static final String CHARACTER_NOT_CONTAIN_SPECIAL_CHARACTER = "^[a-zA-Z0-9\\s|_~`!@#\\$%\\^&\\*\\(\\)_\\-\\+={\\[\\}\\]\\|\\\\:;\"'<,>\\.\\?\\/]+$";

    public static final class EntityStatus {
        public static final int IN_ACTIVE = 0; // Move To Trash
        public static final int ACTIVE = 1; // Active
        public static final int DEACTIVATED = 2; // Deactive

        public EntityStatus() {
        }
    }
}
