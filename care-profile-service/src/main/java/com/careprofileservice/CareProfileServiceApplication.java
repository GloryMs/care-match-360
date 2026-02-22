package com.careprofileservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


@SpringBootApplication(scanBasePackages = {
        "com.careprofileservice",
        "com.carecommon"
})
@EnableDiscoveryClient
public class CareProfileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareProfileServiceApplication.class, args);
    }

}
