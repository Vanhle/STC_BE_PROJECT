package com.stc.project.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";
    private static final String SCHEME = "bearer";
    private static final String BEARER_FORMAT = "JWT";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, 
                            new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme(SCHEME)
                                .bearerFormat(BEARER_FORMAT)
                        )
                )
                .info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
                .title("STC Project API")
                .description("API documentation cho dự án STC - Bao gồm các chức năng xác thực, quản lý người dùng và các tính năng khác")
                .version("1.0.0")
                .contact(new Contact()
                        .name("STC Development Team")
                        .email("dev@stc.com")
                        .url("https://stc.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"))
                .termsOfService("https://stc.com/terms");
    }
}
