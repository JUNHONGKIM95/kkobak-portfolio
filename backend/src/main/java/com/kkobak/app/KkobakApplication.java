package com.kkobak.app;

import com.kkobak.app.media.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class KkobakApplication {
    public static void main(String[] args) {
        SpringApplication.run(KkobakApplication.class, args);
    }
}
