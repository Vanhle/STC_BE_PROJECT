package com.stc.project.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

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
//            if (authentication.getPrincipal() instanceof UserDetails) {
//                userName = ((UserDetails) authentication.getPrincipal()).getUsername();
//            } else if (authentication.getPrincipal() instanceof String) {
//                userName = (String) authentication.getPrincipal();
//            }
            userName = authentication.getName();
        }
        return userName;
    }

    // lấy ra role của người đang login để list ra danh sách
    // Nếu người dùng có role ROLE_MANAGER, thì chỉ nhả ra danh sách người đó tạo
    // Nếu là ROLE_ADMIN, nhả tất dữ liệu
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            if (authority.getAuthority().equals(role)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    public static boolean isManager() {
        return hasRole("ROLE_MANAGER");
    }

}
