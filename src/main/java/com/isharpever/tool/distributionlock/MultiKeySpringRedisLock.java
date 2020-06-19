package com.isharpever.tool.distributionlock;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public class MultiKeySpringRedisLock extends SimpleRedisLock {

    private final static DefaultRedisScript<String> SCRIPT_LOCK = new DefaultRedisScript<>();
    private final static DefaultRedisScript<Long> SCRIPT_UNLOCK = new DefaultRedisScript<>();

    static {
        SCRIPT_LOCK.setResultType(String.class);
        SCRIPT_LOCK.setLocation(new ClassPathResource("script/keys_lock.lua"));
        SCRIPT_LOCK.setScriptText(SCRIPT_LOCK.getScriptAsString());

        SCRIPT_UNLOCK.setResultType(Long.class);
        SCRIPT_UNLOCK.setLocation(new ClassPathResource("script/keys_unlock.lua"));
        SCRIPT_UNLOCK.setScriptText(SCRIPT_UNLOCK.getScriptAsString());
    }

    private StringRedisTemplate stringRedisTemplate;

    /**
     * lockUUID stringRedisTemplate for builder
     */
    @Builder
    public MultiKeySpringRedisLock(List<String> lockKeys, UUID lockUUID,
            StringRedisTemplate stringRedisTemplate, Long waitTime, Long leaseTime,
            TimeUnit timeUnit) {
        super(lockKeys, waitTime, leaseTime, timeUnit);
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    protected String doTryLock(long leaseTimeInMillSeconds) {
        return stringRedisTemplate.execute(SCRIPT_LOCK, lockKeys, lockUUID.toString(),
                String.valueOf(leaseTimeInMillSeconds));
    }

    @Override
    protected long doUnlock() {
        return stringRedisTemplate.execute(SCRIPT_UNLOCK, lockKeys, lockUUID.toString());
    }


}
