package com.EcommerceApp.H2NS.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    @Autowired
    private CacheManager cacheManager;

    @GetMapping("/names")
    public Map<String, Object> getCacheNames() {
        Map<String, Object> response = new HashMap<>();
        response.put("cacheNames", cacheManager.getCacheNames());
        response.put("count", cacheManager.getCacheNames().size());
        return response;
    }

    @DeleteMapping("/clear-all")
    public Map<String, String> clearAllCache() {
        for (String name : cacheManager.getCacheNames()) {
            cacheManager.getCache(name).clear();
        }
        return Map.of(
                "status", "SUCCESS",
                "message", "All cache has been cleared!",
                "clearedCaches", cacheManager.getCacheNames().toString());
    }

    @PostMapping("/disable")
    public Map<String, String> disableCache() {
        for (String name : cacheManager.getCacheNames()) {
            cacheManager.getCache(name).clear();
        }
        return Map.of(
                "status", "DISABLED",
                "message", "Cache has been cleared. Next requests will go to database.");
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
                "status", "ACTIVE",
                "caches", cacheManager.getCacheNames());
    }
}