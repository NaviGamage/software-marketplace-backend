package com.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SoftwareMarketplaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoftwareMarketplaceApplication.class, args);
    }

}
