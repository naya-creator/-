package com.EcommerceApp.H2NS.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Async configuration is already in ThreadPoolConfig
    // This just enables async processing
}