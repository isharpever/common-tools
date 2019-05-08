package com.dianwoda.isharpever.tool.dubbo.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.alibaba.fastjson.JSON;
import java.lang.reflect.Method;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusinessExceptionFilter implements Filter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public BusinessExceptionFilter() {
        logger.info("BusinessExceptionFilter initilizing...");
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation)
            throws RpcException {
        try {
            // fix: dubbo 消费端异步属性会在服务端dubbo调用链路传递一次
            Map<String, String> context = RpcContext.getContext().getAttachments();
            if (context != null) {
                context.remove(Constants.ASYNC_KEY);
            }

            long startTime = System.currentTimeMillis();

            Result result = invoker.invoke(invocation);

            if (result.hasException()
                    && GenericService.class != invoker.getInterface()) {
                try {
                    Throwable exception = result.getException();

                    if (exception instanceof com.dianwoba.core.exception.BusinessException) {
                        return result;
                    }

                    logger.error(
                            "Got exception which called by "
                                    + RpcContext.getContext().getRemoteHost()
                                    + ". service: "
                                    + invoker.getInterface().getName()
                                    + ", method: " + invocation.getMethodName()
                                    + ", time: " + (System.currentTimeMillis() - startTime)
                                    + ", params: " + JSON.toJSONString(invocation.getArguments())
                                    + ", exception: "
                                    + exception.getClass().getName() + ": "
                                    + exception.getMessage(), exception);

                    // 如果是checked异常，直接抛出
                    if (!(exception instanceof RuntimeException)
                            && (exception instanceof Exception)) {
                        return result;
                    }
                    // 在方法签名上有声明，直接抛出
                    try {
                        Method method = invoker.getInterface().getMethod(
                                invocation.getMethodName(),
                                invocation.getParameterTypes());
                        Class<?>[] exceptionClassses = method
                                .getExceptionTypes();
                        for (Class<?> exceptionClass : exceptionClassses) {
                            if (exception.getClass().equals(exceptionClass)) {
                                return result;
                            }
                        }
                    } catch (NoSuchMethodException e) {
                        return result;
                    }

                    // 异常类和接口类在同一jar包里，直接抛出
                    String serviceFile = ReflectUtils.getCodeBase(invoker
                            .getInterface());
                    String exceptionFile = ReflectUtils.getCodeBase(exception
                            .getClass());
                    if (serviceFile == null || exceptionFile == null
                            || serviceFile.equals(exceptionFile)) {
                        return result;
                    }
                    // 是JDK自带的异常，直接抛出
                    String className = exception.getClass().getName();
                    if (className.startsWith("java.")
                            || className.startsWith("javax.")) {
                        return result;
                    }
                    // 是Dubbo本身的异常，直接抛出
                    if (exception instanceof RpcException) {
                        return result;
                    }

                    // 否则，包装成RuntimeException抛给客户端
                    return new RpcResult(new RuntimeException(
                            StringUtils.toString(exception)));
                } catch (Throwable e) {
                    logger.warn("Fail to ExceptionFilter when called by "
                            + RpcContext.getContext().getRemoteHost()
                            + ". service: " + invoker.getInterface().getName()
                            + ", method: " + invocation.getMethodName()
                            + ", params: " + JSON.toJSONString(invocation.getArguments())
                            + ", exception: " + e.getClass().getName() + ": "
                            + e.getMessage(), e);
                    return result;
                }
            }
            return result;
        } catch (RuntimeException e) {
            logger.error(
                    "Got unchecked and undeclared exception which called by "
                            + RpcContext.getContext().getRemoteHost()
                            + ". service: " + invoker.getInterface().getName()
                            + ", method: " + invocation.getMethodName()
                            + ", params: " + JSON.toJSONString(invocation.getArguments())
                            + ", exception: " + e.getClass().getName() + ": "
                            + e.getMessage(), e);
            throw e;
        }
    }
}
