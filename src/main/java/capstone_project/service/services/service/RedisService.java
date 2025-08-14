package capstone_project.service.services.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue);
            log.debug("Successfully saved to Redis with key: {}", key);
        } catch (JsonProcessingException e) {
            log.error("Error serializing value for Redis with key: {}", key, e);
            throw new RuntimeException("Failed to serialize value for Redis", e);
        } catch (Exception e) {
            log.error("Error saving to Redis with key: {}", key, e);
            throw new RuntimeException("Failed to save to Redis", e);
        }
    }

    public void save(String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, timeout, timeUnit);
            log.debug("Successfully saved to Redis with key: {} and timeout: {} {}", key, timeout, timeUnit);
        } catch (JsonProcessingException e) {
            log.error("Error serializing value for Redis with key: {} and timeout", key, e);
            throw new RuntimeException("Failed to serialize value for Redis with timeout", e);
        } catch (Exception e) {
            log.error("Error saving to Redis with key: {} and timeout", key, e);
            throw new RuntimeException("Failed to save to Redis with timeout", e);
        }
    }

    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return null;
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }

            // Value should be a JSON string
            String jsonString = value.toString();
            return objectMapper.readValue(jsonString, clazz);

        } catch (JsonProcessingException e) {
            log.error("Error deserializing value from Redis with key: {} for class: {}", key, clazz.getSimpleName(), e);
            return null;
        } catch (Exception e) {
            log.error("Error getting from Redis with key: {} for class: {}", key, clazz.getSimpleName(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof List<?>) {
            return (List<T>) value;
        }
        return null;
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }

            // Value should be a JSON string
            String jsonString = value.toString();
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return objectMapper.readValue(jsonString, listType);

        } catch (JsonProcessingException e) {
            log.error("Error deserializing list from Redis with key: {} for class: {}", key, clazz.getSimpleName(), e);
            return null;
        } catch (Exception e) {
            log.error("Error getting list from Redis with key: {} for class: {}", key, clazz.getSimpleName(), e);
            return null;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Successfully deleted from Redis with key: {}", key);
        } catch (Exception e) {
            log.error("Error deleting from Redis with key: {}", key, e);
        }
    }

    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Error checking existence in Redis with key: {}", key, e);
            return false;
        }
    }

    public void clearCache() {
        try {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
            log.info("Successfully cleared Redis cache");
        } catch (Exception e) {
            log.error("Error clearing Redis cache", e);
        }
    }

    public void expire(String key, long timeout, TimeUnit timeUnit) {
        try {
            redisTemplate.expire(key, timeout, timeUnit);
            log.debug("Set expiration for key: {} to {} {}", key, timeout, timeUnit);
        } catch (Exception e) {
            log.error("Error setting expiration for key: {}", key, e);
        }
    }

    // Additional utility methods for different data types

    public void saveString(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Successfully saved string to Redis with key: {}", key);
        } catch (Exception e) {
            log.error("Error saving string to Redis with key: {}", key, e);
            throw new RuntimeException("Failed to save string to Redis", e);
        }
    }

    public void saveString(String key, String value, long timeout, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
            log.debug("Successfully saved string to Redis with key: {} and timeout: {} {}", key, timeout, timeUnit);
        } catch (Exception e) {
            log.error("Error saving string to Redis with key: {} and timeout", key, e);
            throw new RuntimeException("Failed to save string to Redis with timeout", e);
        }
    }

    public String getString(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("Error getting string from Redis with key: {}", key, e);
            return null;
        }
    }
}