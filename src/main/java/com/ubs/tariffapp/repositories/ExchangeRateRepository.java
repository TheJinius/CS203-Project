package com.ubs.tariffapp.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import com.ubs.tariffapp.models.ExchangeRates;

@Repository
public class ExchangeRateRepository {

    @Autowired
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ExchangeRateRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public ExchangeRates getExchangeRates(String baseCurrency) {
        String key = "exchange_rates:" + baseCurrency;
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            throw new RuntimeException("Exchange rates not found in Redis for base " + baseCurrency);
        }

        try {
            return objectMapper.readValue(json, ExchangeRates.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse exchange rates JSON", e);
        }
    }
}
