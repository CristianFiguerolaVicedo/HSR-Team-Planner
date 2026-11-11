package com.crifigvic.hsrplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HsrPlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HsrPlannerApplication.class, args);
    }
}
