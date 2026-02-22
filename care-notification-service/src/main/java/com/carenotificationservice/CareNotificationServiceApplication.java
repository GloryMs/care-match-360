package com.carenotificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "com.carenotificationservice",
        "com.carecommon"
})
@EnableDiscoveryClient
@EnableFeignClients
public class CareNotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareNotificationServiceApplication.class, args);
    }

}
