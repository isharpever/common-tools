package com.dianwoda.isharpever.tool.dubbo.filter;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ListenableFilter;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Activate(group = {CommonConstants.CONSUMER, CommonConstants.PROVIDER})
public class InvokeLogFilter extends ListenableFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(InvokeLogFilter.class);

    /** 限定日志中输出参数和结果的最大长度 */
    public static final int DEFAULT_MAX_LENGTH = 1024;

    /** 不输出调用日志的服务 */
    public static final List<String> NOT_LOGGED_SERVICE = Lists.newArrayList(
            "com.alibaba.dubbo.monitor.MonitorService", "org.apache.dubbo.monitor.MonitorService");

    private static final String INVOKELOG_FILTER_START_TIME = "invokelog_filter_start_time";

    public InvokeLogFilter() {
        super.listener = new InvokeLogListener();
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (NOT_LOGGED_SERVICE.contains(invoker.getInterface().getName())) {
            return invoker.invoke(invocation);
        }

        LOGGER.info("side={} remote={} {}.{} start 参数={}",
                side(invoker),
                remote(),
                invoker.getInterface().getName(), invocation.getMethodName(),
                toJSONString(invocation.getArguments()));

        invocation.setAttachment(INVOKELOG_FILTER_START_TIME, String.valueOf(System.currentTimeMillis()));
        return invoker.invoke(invocation);
    }

    private static String toJSONString(Object obj) {
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

    private static String side(Invoker<?> invoker) {
        String result = "unknown";
        try {
            result = invoker.getUrl().getParameter(CommonConstants.SIDE_KEY);
        } catch (Exception e) {
            LOGGER.warn("--- InvokeLogFilter.side error", e);
        }
        return result;
    }

    private static String remote() {
        return RpcContext.getContext().getRemoteAddressString();
    }

    static class InvokeLogListener implements Listener {
        @Override
        public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
            String elapsed = "unknown";
            try {
                String startAttach = invocation.getAttachment(INVOKELOG_FILTER_START_TIME);
                elapsed = String.valueOf(System.currentTimeMillis() - Long.valueOf(startAttach));
            } catch (NumberFormatException e) {
            }

            LOGGER.info("side={} remote={} {}.{} end 参数={} 响应={} 耗时={}",
                    InvokeLogFilter.side(invoker),
                    InvokeLogFilter.remote(),
                    invoker.getInterface().getName(), invocation.getMethodName(),
                    InvokeLogFilter.toJSONString(invocation.getArguments()),
                    InvokeLogFilter.toJSONString(appResponse),
                    elapsed);
        }

        @Override
        public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {

        }
    }
}
