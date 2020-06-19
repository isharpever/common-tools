package com.isharpever.tool.redis;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 缓存操作基类
 *
 * @author yinxiaolin
 * @date 2020/03/13
 */
@Slf4j
public abstract class AbstractCacheManager<VT> {

    /**
     * 表示redis key前缀的环境变量属性名
     */
    private static final String KEY_PREFIX_PROPERTY_NAME = "redis.key.prefix";

    /**
     * 影子链路key前缀
     */
    private static final String SHADOW_PREFIX = "__test_";

    @Autowired
    private Environment environment;

    /**
     * 写入redis
     *
     * @param key
     * @param value
     * @param timeout 过期时间(秒)
     */
    public void put(String key, VT value, int timeout) {
        try {
            getRedisTemplate().opsForValue().set(this.buildRedisKey(key), value, timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("--- CacheManager.put异常 key={} value={} timeout={}",
                    key, JSON.toJSONString(value), timeout);
        }
    }

    public VT get(String key) {
        try {
            return getRedisTemplate().opsForValue().get(this.buildRedisKey(key));
        } catch (Exception e) {
            log.error("--- CacheManager.get异常 key={}", key);
        }
        return null;
    }

    public List<VT> mget(List<String> keys) {
        try {
            return getRedisTemplate().opsForValue().multiGet(this.buildRedisKey(keys));
        } catch (Exception e) {
            log.error("--- CacheManager.mget异常 keys={}", keys);
        }
        return null;
    }

    public void del(String key) {
        try {
            getRedisTemplate().delete(this.buildRedisKey(key));
        } catch (Exception e) {
            log.error("--- CacheManager.delete异常 key={}", key);
        }
    }

    public boolean putAbsentWithExpire(String key, VT value, long expire, TimeUnit timeUnit) {
        try {
            if (getRedisTemplate().opsForValue().setIfAbsent(key, value)) {
                getRedisTemplate().expire(key, expire, timeUnit);
                return true;
            }
        } catch (Exception e) {
            log.error("--- CacheManager.putAbsentWithExpire异常 key={} value={} expire={} timeUnit={}",
                    key, JSON.toJSONString(value), expire, timeUnit);
        }
        return false;
    }

    /**
     * 指定操作缓存用的RedisTemplate实例,抽象方法,由子类实现
     */
    protected abstract RedisTemplate<String, VT> getRedisTemplate();

    public String buildRedisKey(String key) {
        String realKey = key;
        String keyPrefix = environment.getProperty(KEY_PREFIX_PROPERTY_NAME);
        if (StringUtils.isNotBlank(keyPrefix)) {
            realKey = String.format("%s:%s", keyPrefix, key);
        }
        return realKey;
    }

    private List<String> buildRedisKey(List<String> keys) {
        List<String> results = Lists.newArrayListWithCapacity(keys.size());
        for (String key : keys) {
            results.add(buildRedisKey(key));
        }
        return results;
    }
}
