package com.carematchservice.config;


import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.carematchservice.feign")
public class FeignConfig {
    // Feign client configuration
}
