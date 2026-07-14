package com.digitalwallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entry point for the digital wallet backend.
 *
 * Spring Boot will scan all subpackages of com.digitalwallet, including
 * config, security, api, domain, and websocket.
 */
@SpringBootApplication
public class WalletCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletCoreApplication.class, args);
    }
}
