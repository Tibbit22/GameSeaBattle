package com.example.demo.config;  // Соответствует расположению

import com.example.demo.model.Move;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {
    @Bean
    public Cache<Long, List<Move>> movesCache() {
        return Caffeine.newBuilder()
                .maximumSize(100)             // Максимум 1000 игр в кэше
                .expireAfterWrite(24, TimeUnit.HOURS) // Храним ходы 24 часа
                .expireAfterAccess(5, TimeUnit.HOURS) // Удаляем если не было обращений 6 часов
                .recordStats()                 // Для мониторинга
                .build();
    }
}
