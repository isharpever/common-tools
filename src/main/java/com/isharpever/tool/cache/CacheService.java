package com.isharpever.tool.cache;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;

/**
 * @author lijf
 */
public class CacheService {

    private static final Logger LOG = LoggerFactory.getLogger(CacheService.class);

    private final static DefaultRedisScript<Long> SCRIPT_INCR_EXPIR = new DefaultRedisScript<>();
    static {
        SCRIPT_INCR_EXPIR.setResultType(Long.class);
        SCRIPT_INCR_EXPIR.setLocation(new ClassPathResource("script/incrExpire.lua"));
        SCRIPT_INCR_EXPIR.setScriptText(SCRIPT_INCR_EXPIR.getScriptAsString());
    }

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    public Long opsForSetAddReturnSize(String key, long riderId) {
        try {
            redisTemplate.opsForSet().add(key, riderId);
            long num = redisTemplate.opsForSet().size(key);
            if (num < 5) {
                redisTemplate.expire(key, 1, TimeUnit.DAYS);
            }
            return num;
        } catch (Exception e) {
            LOG.error("opsForSetAddReturnSize fail key:{} riderId:{}", key, riderId, e);
            return 0L;
        }
    }

    public Boolean putCacheWithExpireIfNotExist(String key, long mills) {
        try {
            return stringRedisTemplate.opsForValue().setIfAbsent(key, "1", mills, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOG.error("putCacheWithExpireIfNotExist fail key:{} mills:{}", key, mills, e);
            return false;
        }
    }

    public void putCacheWithExpire(String key, Object value, int expire) {
        try {
            redisTemplate.opsForValue().set(key, value, expire, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.error("PUT cache exception [key=" + key + ", value=" + value + ", expire=" + expire + "].", e);
        }
    }

    public void putCache(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            LOG.error("PUT cache exception [key=" + key + ", value=" + value + "].", e);
        }
    }

    public Object getCache(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            LOG.error("GET cache exception [key=" + key + "].", e);
        }
        return null;
    }

    public List<Object> getCache(List<String> keys) {
        try {
            return redisTemplate.opsForValue().multiGet(keys);
        } catch (Exception e) {
            LOG.error("GET cache exception [keys=" + keys + "].", e);
        }
        return new ArrayList<>();
    }

    public void removeCache(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            LOG.error("Remove cache exception [key=" + key + "].", e);
        }
    }

    public Long increment(String key, long delta) {
        try {
            return stringRedisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            LOG.error("increment cache exception key={}", e);
        }
        return null;
    }

    public Long increment(String h, String hk, long delta) {
        try {
            return stringRedisTemplate.opsForHash().increment(h, hk, delta);
        } catch (Exception e) {
            LOG.error("increment hash cache exception h={} hk={}", e);
        }
        return null;
    }

    public Map<String, String> getHashEntries(String h) {
        try {
            HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
            return hashOperations.entries(h);
        } catch (Exception e) {
            LOG.error("GET hash entries exception h={}", e);
        }
        return null;
    }

    /**
     * 设置key的过期时间
     * @param key
     * @param timeout 过期时间(秒)
     * @return
     */
    public Boolean expire(String key, long timeout) {
        try {
            return stringRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.error("expire cache exception [key=" + key + "].", e);
        }
        return null;
    }


    public Map<String, Object> batchQueryKeys(List<String> keys) {
        Map<String, Object> result = Maps.newHashMap();
        try {
            if (CollectionUtils.isEmpty(keys)) {
                return result;
            }
            List<Object> values = this.redisTemplate.executePipelined(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    for(String key:keys) {
                        connection.get(key.getBytes());
                    }
                    return null;
                }
            }, new JdkSerializationRedisSerializer());

            if (CollectionUtils.isEmpty(keys)) {
                return result;
            }

            for (int i = 0, size = keys.size(); i < size; i++) {
                result.put(keys.get(i), values.get(i));
            }
            return result;
        } catch (Exception e) {
            LOG.error("--- redis executePipelined exception keys{} cause:{}", JSON.toJSONString(keys), e);
        }
        return result;
    }

    public List<Object> mget(List<String> keys) {
        try {
            return  this.redisTemplate.opsForValue().multiGet(keys);
        } catch (Exception e) {
            LOG.error("--- redis mget exception keys{} cause:{}", JSON.toJSONString(keys), e);
            return null;
        }
    }

    public boolean putAbsentWithExpire(String key, Object value, int expire, TimeUnit timeUnit) {
        try {
            if(redisTemplate.opsForValue().setIfAbsent(key, value)) {
                redisTemplate.expire(key, expire, timeUnit);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOG.error("--- putAbsentWithExpire exception [key=" + key + ", value=" + value + ", expire=" + expire + "].", e);
            return false;
        }
    }

    public Long incrExpir(String key, long timeout, TimeUnit unit) {
        try {
            Long timeOutMillis = unit.toMillis(timeout);
            return stringRedisTemplate.execute(SCRIPT_INCR_EXPIR, Lists.newArrayList(key), timeOutMillis.toString());
        } catch (Exception e) {
            LOG.error("incr cache exception [key=" + key + "].", e);
        }
        return null;
    }

    public void putStringCache(String key, String value, int expire) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, (long)expire, TimeUnit.SECONDS);
        } catch (Exception var5) {
            LOG.error("PUT String cache exception [key=" + key + ", value=" + value + ", expire=" + expire + "].", var5);
        }

    }

    public String getStringValue(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception var3) {
            LOG.error("GET String value exception [key=" + key + "].", var3);
            return null;
        }
    }

    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }
}
