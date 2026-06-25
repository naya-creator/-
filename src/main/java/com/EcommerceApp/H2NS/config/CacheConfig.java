package com.EcommerceApp.H2NS.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling // Enable scheduled jobs for cache cleanup
public class CacheConfig {
    // Additional cache configuration if needed
    // This works with RedisConfig.java
}