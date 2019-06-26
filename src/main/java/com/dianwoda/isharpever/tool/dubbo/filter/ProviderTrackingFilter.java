package com.dianwoda.isharpever.tool.dubbo.filter;

import com.dianwoda.isharpever.tool.mdc.LogUniqueKeyUtil;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Activate(group = {CommonConstants.PROVIDER}, order = -30000)
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
