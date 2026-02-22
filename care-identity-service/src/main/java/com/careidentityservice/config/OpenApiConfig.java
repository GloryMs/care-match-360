package com.careidentityservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CareMatch360 Identity Service API")
                        .version("1.0.0")
                        .description("Authentication and User Management Service for CareMatch360 Platform")
                        .contact(new Contact()
                                .name("CareMatch360 Team")
                                .email("support@carematch360.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://carematch360.com/license")));
    }
}