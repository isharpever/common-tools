package com.isharpever.tool.distributionlock;

public interface LockCallback {

    Object doWithLock(LockContext lockContext);

}
