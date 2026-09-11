package com.biopet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class BiopetApplication {
    /**
     * Starts the BIOPET Spring Boot application.
     *
     * @param args command line arguments forwarded to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(BiopetApplication.class, args);
    }
}
