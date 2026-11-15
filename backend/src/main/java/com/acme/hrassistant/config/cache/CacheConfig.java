package com.acme.hrassistant.config.cache;

import com.acme.hrassistant.config.AppProperties;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String POLICIES = "policies";
    public static final String HOLIDAYS = "holidays";

    @Bean
    CacheManager cacheManager(AppProperties appProperties) {
        CaffeineCacheManager manager = new CaffeineCacheManager(POLICIES, HOLIDAYS);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(
                        Math.max(1, appProperties.getCache().getPoliciesTtl().toSeconds()),
                        TimeUnit.SECONDS)
                .recordStats());
        // Holidays use the same manager defaults; TTL for holidays is also covered by expireAfterWrite.
        // Prefer the longer of the two TTLs so holiday cache is not shorter than configured.
        long holidaysSeconds = Math.max(1, appProperties.getCache().getHolidaysTtl().toSeconds());
        long policiesSeconds = Math.max(1, appProperties.getCache().getPoliciesTtl().toSeconds());
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Math.max(holidaysSeconds, policiesSeconds), TimeUnit.SECONDS)
                .recordStats());
        return manager;
    }
}
