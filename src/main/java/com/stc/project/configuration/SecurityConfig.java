package com.stc.project.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

import java.io.IOException;
import java.time.LocalDateTime;

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {

    CustomJwtDecoder jwtDecoder;
    CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    /**
     * Cấu hình CORS để cho phép client truy cập
     * @Author: @Vanhledeptrai123321
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173","http://localhost:4173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*") );
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.cors(cors -> cors.configurationSource(corsConfigurationSource())); // Thêm dòng này để bật CORS

        // Config các endpoint cho phép truy cập
        httpSecurity.authorizeHttpRequests(request ->
                request
                        // Swagger endpoints - phải đặt đầu tiên, không chỉ GET
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // Auth endpoints
                        .requestMatchers(HttpMethod.POST, "/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/**").permitAll()
                        .requestMatchers("/api/projects/**").authenticated()
                        .requestMatchers("/api/buildings/**").authenticated()
                        .requestMatchers("/api/apartments/**").authenticated()
                        .requestMatchers("/api/users/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/test/*").hasRole("ADMIN")
                        .anyRequest().authenticated());

        // Chỉ áp dụng JWT cho các endpoint cần xác thực
        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer -> jwtConfigurer.decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(customAuthenticationEntryPoint));

        httpSecurity.csrf(csrf -> csrf.disable());

        // Tắt session management để đảm bảo stateless
        httpSecurity.sessionManagement(session ->
            session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS));

        return httpSecurity.build();
    }


    /**
     * Method này sẽ chạy nếu như việc xác thực không thành công.
     * Sẽ trả ra exception
     **/
//    @Bean
//    public AuthenticationEntryPoint jwtAuthenticationEntryPoint() {
//        return new AuthenticationEntryPoint() {
//            @Override
//            public void commence(
//                    HttpServletRequest request,
//                    HttpServletResponse response,
//                    AuthenticationException authException
//            ) throws IOException {
//                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
//            }
//        };
//    }

    /**
     * trích xuất các quyền hạn từ yêu cầu "scope" của JWT
     * và chuyển đổi chúng thành một đối tượng Authentication.
     **/
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("");  // Không thêm prefix
        authoritiesConverter.setAuthoritiesClaimName("scope");  // Lấy từ scope claim

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
