package com.isharpever.tool.distributionlock;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleRedisLock {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRedisLock.class);
    private static final int ONE_SECOND = 1000;
    /**
     * 默认锁的过期时间
     */
    public static final long DEFAULT_EXPIRY_TIME_MILLIS = 3 * ONE_SECOND;
    /**
     * 默认请求获得锁超时时间
     */
    public static final long DEFAULT_ACQUIRE_TIMEOUT_MILLIS = 3 * ONE_SECOND;
    public static final long DEFAULT_ACQUIRY_RESOLUTION_MILLIS = 10;

    protected List<String> lockKeys;
    protected final UUID lockUUID;

    protected Long waitTime = DEFAULT_ACQUIRE_TIMEOUT_MILLIS;
    protected Long leaseTime = DEFAULT_EXPIRY_TIME_MILLIS;
    protected TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    public final static String LOCK_SUCCESS = "OK";
    public final static String LOCK_FAIL = "FAIL";


    public SimpleRedisLock(String lockKey, Long waitTime, Long leaseTime, TimeUnit timeUnit) {
        this(Collections.singletonList(lockKey), waitTime, leaseTime, timeUnit);
    }

    public SimpleRedisLock(List<String> lockKeys, Long waitTime, Long leaseTime,
            TimeUnit timeUnit) {
        this.lockKeys = lockKeys;
        this.lockUUID = UUID.randomUUID();
        if (waitTime != null) {
            this.waitTime = waitTime;
        }
        if (leaseTime != null) {
            this.leaseTime = leaseTime;
        }
        if (timeUnit != null) {
            this.timeUnit = timeUnit;
        }
    }

    /**
     * 使用更便捷
     */
    public Object doWithLock(LockCallback callback) {
        LockContext context = new LockContext();
        try {
            String result = tryLock();
            context.setSuccess(LOCK_SUCCESS.equalsIgnoreCase(result));
            context.setResult(result);
        } catch (Exception e) {
            LOGGER.error("tryLock occurs error", e);
            unlock();
        } finally {
            try {
                return callback.doWithLock(context);
            } catch (Exception e) {
                throw e;
            } finally {
                if (context.isSuccess()) {
                    unlock();
                }
            }
        }
    }

    /**
     * 尝试获得锁，默认超时时间3s，默认加锁时间3s
     */
    public String tryLock() throws InterruptedException {
        return tryLock(waitTime, leaseTime, timeUnit);
    }

    /**
     * 释放锁
     * @return
     */
    public long unlock() {
        return doUnlock();
    }

    /**
     * 尝试获得锁
     *
     * @param waitTime 获得锁超时时间，不需要就传0
     * @param leaseTime 获得锁加锁时间
     * @param unit 时间单位
     */
    private String tryLock(long waitTime, long leaseTime, TimeUnit unit)
            throws InterruptedException {
        String result = LOCK_FAIL;
        long timeout = unit.toMillis(waitTime);
        long internalLockLeaseTime = unit.toMillis(leaseTime);

        while (timeout >= 0) {
            result = doTryLock(internalLockLeaseTime);
            if (LOCK_SUCCESS.equalsIgnoreCase(result)) {
                return LOCK_SUCCESS;
            }
            timeout -= DEFAULT_ACQUIRY_RESOLUTION_MILLIS;
            if (timeout > 0) {
                Thread.sleep(DEFAULT_ACQUIRY_RESOLUTION_MILLIS);
            }
        }
        return result;
    }

    protected abstract String doTryLock(long leaseTimeInMillSeconds);

    /**
     * 释放锁
     */
    protected abstract long doUnlock();
}
