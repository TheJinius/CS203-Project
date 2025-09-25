package com.ubs.tariffapp.repositories;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

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

    public ExchangeRates getExchangeRates() {
        String key = "exchange_rates:USD";
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            throw new RuntimeException("Error fetching exchange rates from redis");
        }

        try {
            ExchangeRates er = new ExchangeRates();
            er.setRates(objectMapper.readValue(json, new TypeReference<Map<String, Double>>() {}));
            return er;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse exchange rates JSON", e);
        }
    }
}
