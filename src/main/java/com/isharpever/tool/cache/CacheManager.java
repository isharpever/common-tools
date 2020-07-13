package com.isharpever.tool.cache;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;

/**
 * redis缓存, 对业务key附加前缀
 *
 * @author yinxiaolin
 * @date 2018/03/26
 */
public class CacheManager {

    /**
     * key前缀默认值,使用者应调用setKeyPrefix设置有独特业务含义的值
     */
    private String keyPrefix = "default";

    @Resource
    private CacheService cacheService;

    public String buildRedisKey(String key) {
        return String.format("%s_%s", this.getKeyPrefix(), key);
    }

    public List<String> batchBuildRedisKey(List<String> keys) {
        List<String> results = Lists.newArrayListWithCapacity(keys.size());
        for (String key : keys) {
            results.add(buildRedisKey(key));
        }
        return results;
    }

    public Object get(String key) {
        return cacheService.getCache(this.buildRedisKey(key));
    }

    public void put(String key, Object value) {
        cacheService.putCache(this.buildRedisKey(key), value);
    }


    public Long opsForSetAddReturnSize(String key, long riderId) {
       return cacheService.opsForSetAddReturnSize(key, riderId);
    }

    public Boolean putCacheWithExpireIfNotExist(String key, long mills) {
        return cacheService.putCacheWithExpireIfNotExist(key, mills);
    }

    /**
     * 写入redis
     *
     * @param key
     * @param value
     * @param timeout 过期时间(秒)
     */
    public void put(String key, Object value, int timeout) {
        cacheService.putCacheWithExpire(this.buildRedisKey(key), value, timeout);
    }

    public void del(String key) {
        cacheService.removeCache(this.buildRedisKey(key));
    }

    /**
     * 指定key的value增加指定值,并设置过期时间
     * @param key
     * @param delta
     * @param timeout 过期时间(秒)
     * @return
     */
    public Long increment(String key, long delta, int timeout) {
        Long result = cacheService.increment(this.buildRedisKey(key), delta);
        this.expire(key, timeout);
        return result;
    }

    /**
     * 指定hash key的value增加指定值
     * @param h
     * @param hk
     * @param delta
     * @return
     */
    public Long increment(String h, String hk, long delta) {
        return cacheService.increment(this.buildRedisKey(h), hk, delta);
    }

    /**
     * 指定hash key的value增加指定值
     * @param h
     * @param hk
     * @param delta
     * @param timeout 过期时间(秒)
     * @return
     */
    public Long increment(String h, String hk, long delta, int timeout) {
        Long result = cacheService.increment(this.buildRedisKey(h), hk, delta);
        this.expire(h, timeout);
        return result;
    }

    public Map<String, String> getHashEntries(String h) {
        return cacheService.getHashEntries(this.buildRedisKey(h));
    }

    /**
     * 指定key的过期时间
     * @param key
     * @param timeout 过期时间(秒)
     * @return
     */
    public Boolean expire(String key, int timeout) {
        return cacheService.expire(this.buildRedisKey(key), timeout);
    }

    public Map<String, Object> batchQueryKeys(List<String> keys) {
        List<String> buildKeys = batchBuildRedisKey(keys);
        return cacheService.batchQueryKeys(buildKeys);
    }

    public List<Object> mget(List<String> keys) {
        List<String> buildKeys = batchBuildRedisKey(keys);
        return cacheService.mget(buildKeys);
    }

    public boolean putAbsentWithExpire(String key, Object value, int expire, TimeUnit timeUnit) {
        return cacheService.putAbsentWithExpire(buildRedisKey(key), value, expire, timeUnit);
    }

    public Long incrExpir(String key, long timeout, TimeUnit unit) {
        return cacheService.incrExpir(this.buildRedisKey(key), timeout, unit);
    }

    public String getStringValue(String key) {
        return cacheService.getStringValue(this.buildRedisKey(key));
    }

    /**
     * 写入redis
     *
     * @param key
     * @param value
     * @param timeout 过期时间(秒)
     */
    public void putString(String key, String value, int timeout) {
        cacheService.putStringCache(this.buildRedisKey(key), value, timeout);
    }

    protected String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
}
