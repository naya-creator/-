package com.EcommerceApp.H2NS.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

        @Bean
        public RedisConnectionFactory redisConnectionFactory() {
                // Connect to Redis on localhost:6379
                return new LettuceConnectionFactory("localhost", 6379);
        }

        @Bean
        public RedisTemplate<String, Object> redisTemplate() {
                RedisTemplate<String, Object> template = new RedisTemplate<>();
                template.setConnectionFactory(redisConnectionFactory());

                // Configure JSON serialization so we can store objects
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
                mapper.activateDefaultTyping(
                                BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
                                ObjectMapper.DefaultTyping.NON_FINAL);

                GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

                template.setKeySerializer(new StringRedisSerializer());
                template.setValueSerializer(serializer);
                template.setHashKeySerializer(new StringRedisSerializer());
                template.setHashValueSerializer(serializer);

                template.afterPropertiesSet();
                return template;
        }

        @Bean
        public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.activateDefaultTyping(
                                BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
                                ObjectMapper.DefaultTyping.NON_FINAL);

                GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(10))
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                                .disableCachingNullValues();

                Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

                cacheConfigurations.put("products", RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(5))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(serializer)));

                cacheConfigurations.put("topProducts", RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(15))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(serializer)));

                cacheConfigurations.put("topSellingProducts", RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(30))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                .fromSerializer(serializer)));

                return RedisCacheManager.builder(redisConnectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withInitialCacheConfigurations(cacheConfigurations)
                                .build();
        }
}