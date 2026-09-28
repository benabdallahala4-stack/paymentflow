package com.paymentflow.shared.infrastructure;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import java.time.Duration;

/**
 * Cache-aside for account balance READS only (rule 7 / AGENTS.md invariant).
 *
 * <p>IMPORTANT: financial correctness never depends on this cache. The transfer
 * (write) path in payment/application always reads the account directly from
 * PostgreSQL with @Version optimistic locking and never trusts a cached balance for
 * validation - see AccountBalanceCacheService. This cache exists purely to speed up
 * repeated GET /accounts/{id} reads and uses a short TTL so staleness is bounded and
 * cheap to tolerate.
 */
@Configuration
@EnableCaching
public class RedisConfig {

    public static final String ACCOUNT_BALANCE_CACHE = "accountBalance";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(10))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .build();
    }
}
