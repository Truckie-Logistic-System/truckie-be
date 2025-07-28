package capstone_project.service.services.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void save(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof List<?>) {
            return (List<T>) value;
        }
        return null;
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void clearCache() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }
}