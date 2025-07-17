package com.stc.project.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * Get the login of the current user.
     *
     * @return the login of the current user
     */
    // truy xuất thông tin người dùng từ SecurityContext
    // Trả về username của người dùng đang đăng nhập hiện tại.
    public static String getCurrentUserLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // chứa thông tin người dùng (authenticated).
        String userName = null;
        if (authentication != null) {
            // Khi xác thực thành công bằng UserDetailsService, getPrincipal() sẽ trả về 1 object kiểu UserDetails.
            if (authentication.getPrincipal() instanceof UserDetails) {
                userName = ((UserDetails) authentication.getPrincipal()).getUsername();
            } else if (authentication.getPrincipal() instanceof String) {
                userName = (String) authentication.getPrincipal();
            }
        }
        return userName;
    }
}
