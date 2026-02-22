package com.carematchservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "com.carematchservice",
        "com.carecommon"
})
@EnableDiscoveryClient
@EnableFeignClients
public class CareMatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareMatchServiceApplication.class, args);
    }

}
