package com.dianwoda.isharpever.tool.dubbo.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.dianwoda.isharpever.tool.mdc.LogUniqueKeyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Activate(
        group = {Constants.PROVIDER},
        order = -30000
)
public class ProviderTrackingFilter implements Filter {
    private Logger logger = LoggerFactory.getLogger(ProviderTrackingFilter.class);

    public ProviderTrackingFilter() {
        logger.info("ProviderTrackingFilter initilizing...");
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            LogUniqueKeyUtil.generateKeyToLog();
        } catch (Throwable var4) {
            logger.warn("ProviderTrackingFilter error", var4);
        }

        return  invoker.invoke(invocation);
    }
}
