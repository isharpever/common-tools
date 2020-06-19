package com.isharpever.tool.distributionlock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public class SpringRedisLockV2 extends SimpleRedisLock {

    private final static DefaultRedisScript<String> SCRIPT_LOCK = new DefaultRedisScript<>();
    private final static DefaultRedisScript<Long> SCRIPT_UNLOCK = new DefaultRedisScript<>();

    static {
        SCRIPT_LOCK.setResultType(String.class);
        SCRIPT_LOCK.setLocation(new ClassPathResource("script/lock.lua"));
        SCRIPT_LOCK.setScriptText(SCRIPT_LOCK.getScriptAsString());

        SCRIPT_UNLOCK.setResultType(Long.class);
        SCRIPT_UNLOCK.setLocation(new ClassPathResource("script/unlock.lua"));
        SCRIPT_UNLOCK.setScriptText(SCRIPT_UNLOCK.getScriptAsString());

    }

    private StringRedisTemplate stringRedisTemplate;

    /**
     * @param lockUUID 单纯为了builder写的，不需要传
     * @param stringRedisTemplate 单纯为了builder写的，不需要传
     */
    @Builder
    public SpringRedisLockV2(String lockKey, UUID lockUUID, StringRedisTemplate stringRedisTemplate,
            Long waitTime, Long leaseTime, TimeUnit timeUnit) {
        super(lockKey, waitTime, leaseTime, timeUnit);
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    protected String doTryLock(long leaseTimeInMillSeconds) {
        List<String> keys = new ArrayList<>();
        keys.add(lockKeys.get(0));
        return stringRedisTemplate.execute(SCRIPT_LOCK, keys, lockUUID.toString(),
                String.valueOf(leaseTimeInMillSeconds));
    }

    /**
     * 释放锁
     */
    @Override
    protected long doUnlock() {
        List<String> keys = new ArrayList<>();
        keys.add(lockKeys.get(0));
        return stringRedisTemplate.execute(SCRIPT_UNLOCK, keys, lockUUID.toString());
    }
}
