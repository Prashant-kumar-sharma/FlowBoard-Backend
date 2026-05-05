package com.flowboard.list.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {
    private static final List<String> LIST_CACHE_NAMES = List.of(
            "list:byId",
            "list:byBoard",
            "list:archivedByBoard");

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = redisSerializer();
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            @Value("${spring.cache.redis.time-to-live:10m}") Duration timeToLive) {
        GenericJackson2JsonRedisSerializer serializer = redisSerializer();
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(timeToLive)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .transactionAware()
                .build();
    }

    private GenericJackson2JsonRedisSerializer redisSerializer() {
        return new GenericJackson2JsonRedisSerializer()
                .configure(objectMapper -> {
                    objectMapper.findAndRegisterModules();
                    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                });
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Ignoring Redis cache get failure for cache={} key={}", cacheName(cache), key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Ignoring Redis cache put failure for cache={} key={}", cacheName(cache), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Ignoring Redis cache evict failure for cache={} key={}", cacheName(cache), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Ignoring Redis cache clear failure for cache={}", cacheName(cache), exception);
            }

            private String cacheName(Cache cache) {
                return cache != null ? cache.getName() : CacheManager.class.getSimpleName();
            }
        };
    }

    @Bean
    public ApplicationRunner listCacheWarmupEviction(
            CacheManager cacheManager,
            @Value("${app.cache.clear-list-caches-on-startup:true}") boolean clearListCachesOnStartup) {
        return args -> {
            if (!clearListCachesOnStartup) {
                return;
            }
            LIST_CACHE_NAMES.forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
            log.info("Cleared list Redis caches on startup to remove legacy serialized entries");
        };
    }
}
