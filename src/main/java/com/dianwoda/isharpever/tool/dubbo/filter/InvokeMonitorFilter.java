package com.dianwoda.isharpever.tool.dubbo.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.fastjson.JSON;
import com.dianwoba.monitor.client.MonitorFactory;
import com.dianwoba.monitor.client.MonitorPoint;
import com.dianwoba.monitor.client.MonitorUtil;
import com.dianwoda.isharpever.tool.utils.NetUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Activate(group = {Constants.PROVIDER})
public class InvokeMonitorFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(InvokeMonitorFilter.class);

    private MonitorUtil monitor = MonitorFactory.connect();

    public InvokeMonitorFilter() {
        LOGGER.info("InvokeMonitorFilter initilizing...");
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        long start = System.currentTimeMillis();
        Result result = invoker.invoke(invocation);

        try {
            // 监控返回结果大小和RT
            monitorResultLengthAndRt(invoker, invocation, result,
                    System.currentTimeMillis() - start);
        } catch (Throwable e) {
            LOGGER.warn("InvokeMonitorFilter error", e);
        }
        return result;
    }

    /**
     * 监控返回结果大小和RT
     * @param invoker
     * @param invocation
     * @param result
     * @param cost
     */
    private void monitorResultLengthAndRt(Invoker<?> invoker, Invocation invocation, Result result,
            long cost) {
        String ip = NetUtil.getLocalHostAddress();
        String interfaceName = invoker.getInterface().getName();
        String methodName = invocation.getMethodName();

        int rsltLen = -1;
        try {
            String strResult = JSON.toJSONString(result);
            if (StringUtils.isNotBlank(strResult)) {
                rsltLen = strResult.getBytes("utf-8").length;
            }
        } catch (Exception e) {
            LOGGER.warn("--- 计算dubbo返回结果大小失败", e);
        }

        MonitorPoint point = MonitorPoint
                .monitorKey("isharpever.dubbo.monitor")
                .addTag("app", this.getAppCode(invoker))
                .addTag("ip", ip)
                .addTag("interface", interfaceName)
                .addTag("method", methodName)
                .addField("cost", cost)
                .addField("rlen", rsltLen).build();
        monitor.writePoint(point);
    }

    /**
     * 返回应用名
     */
    private String getAppCode(Invoker<?> invoker) {
        String appCode = invoker.getUrl().getParameter("application");
        if (StringUtils.isBlank(appCode)) {
            appCode = "unknown";
        }
        return appCode;
    }
}
