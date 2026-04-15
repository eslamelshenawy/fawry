package com.fawry.routing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableRetry
public class PaymentRoutingApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentRoutingApplication.class, args);
    }
}
