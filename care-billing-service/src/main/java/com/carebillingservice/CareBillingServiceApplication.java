package com.carebillingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
        "com.carebillingservice",
        "com.carecommon"
})
@EnableDiscoveryClient
public class CareBillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareBillingServiceApplication.class, args);
    }

}
