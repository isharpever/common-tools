package com.isharpever.tool.distributionlock;

import lombok.Getter;
import lombok.Setter;

public class LockContext {

    private boolean success;

    /**
     * 加锁失败时，保存导致加锁失败对应的key
     */
    @Setter
    @Getter
    private Object result;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
