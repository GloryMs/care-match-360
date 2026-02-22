package com.careidentityservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {"com.careidentityservice", "com.carecommon"})
@EnableDiscoveryClient
@SpringBootApplication
public class CareIdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareIdentityServiceApplication.class, args);
    }

}
