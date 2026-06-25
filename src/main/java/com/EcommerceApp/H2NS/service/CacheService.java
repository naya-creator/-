package com.EcommerceApp.H2NS.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void put(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void put(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public Long incrementCounter(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long getCounter(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    public void addToSortedSet(String key, Object value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }

    public Set<Object> getTopFromSortedSet(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    public void evictPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public Set<String> getKeys(String pattern) {
        return redisTemplate.keys(pattern);
    }
}