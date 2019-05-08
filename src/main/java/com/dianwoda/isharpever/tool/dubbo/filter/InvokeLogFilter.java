package com.dianwoda.isharpever.tool.dubbo.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Activate(group = {Constants.CONSUMER, Constants.PROVIDER})
public class InvokeLogFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(InvokeLogFilter.class);

    /** 限定日志中输出参数和结果的最大长度 */
    public static final int DEFAULT_MAX_LENGTH = 1024;

    /** 不输出调用日志的服务 */
    public static final List<String> NOT_LOGGED_SERVICE = Lists.newArrayList(
            "com.alibaba.dubbo.monitor.MonitorService");

    public InvokeLogFilter() {
        LOGGER.info("InvokeLogFilter initilizing...");
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (NOT_LOGGED_SERVICE.contains(invoker.getInterface().getName())) {
            return invoker.invoke(invocation);
        }

        LOGGER.info("side={} {}.{} start 参数={}",
                this.side(invoker),
                invoker.getInterface().getName(), invocation.getMethodName(),
                toJSONString(invocation.getArguments()));

        long start = System.currentTimeMillis();
        Result result = invoker.invoke(invocation);

        try {
            LOGGER.info("side={} {}.{} end 参数={} 响应={} 耗时={}",
                    this.side(invoker),
                    invoker.getInterface().getName(), invocation.getMethodName(),
                    toJSONString(invocation.getArguments()),
                    toJSONString(result),
                    System.currentTimeMillis() - start);
        } catch (Throwable e) {
            LOGGER.warn("InvokeLogFilter error. side={}", this.side(invoker), e);
        }
        return result;
    }

    private String toJSONString(Object obj) {
        String result = "unknown";
        try {
            result = JSON.toJSONString(obj);
            if (StringUtils.isNotBlank(result) && result.length() > DEFAULT_MAX_LENGTH) {
                result = result.substring(0, DEFAULT_MAX_LENGTH);
            }
        } catch (Exception e) {
            LOGGER.warn("--- InvokeLogFilter.toJSONString error", e);
        }
        return result;
    }

    private String side(Invoker<?> invoker) {
        String result = "unknown";
        try {
            result = invoker.getUrl().getParameter(Constants.SIDE_KEY);
        } catch (Exception e) {
            LOGGER.warn("--- InvokeLogFilter.side error", e);
        }
        return result;
    }
}
