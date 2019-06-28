package com.isharpever.tool.dubbo.filter;

import com.alibaba.fastjson.JSON;
import com.dianwoba.monitor.client.MonitorFactory;
import com.dianwoba.monitor.client.MonitorPoint;
import com.dianwoba.monitor.client.MonitorUtil;
import com.isharpever.tool.utils.NetUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ListenableFilter;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Activate(group = {CommonConstants.PROVIDER})
public class InvokeMonitorFilter extends ListenableFilter {
    private static final String INVOKEMONITOR_FILTER_START_TIME = "invokemonitor_filter_start_time";

    public InvokeMonitorFilter() {
        super.listener = new InvokeMonitorListener();
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        invocation.setAttachment(INVOKEMONITOR_FILTER_START_TIME, String.valueOf(System.currentTimeMillis()));
        return invoker.invoke(invocation);
    }

    static class InvokeMonitorListener implements Listener {
        private static final Logger LOGGER = LoggerFactory.getLogger(InvokeMonitorFilter.class);

        private MonitorUtil monitor = MonitorFactory.connect();

        @Override
        public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
            Long cost = null;
            try {
                String startAttach = invocation.getAttachment(INVOKEMONITOR_FILTER_START_TIME);
                cost = System.currentTimeMillis() - Long.valueOf(startAttach);
            } catch (NumberFormatException e) {
            }

            // 监控返回结果大小和RT
            monitorResultLengthAndRt(invoker, invocation, appResponse, cost);
        }

        @Override
        public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {

        }

        /**
         * 监控返回结果大小和RT
         * @param invoker
         * @param invocation
         * @param result
         * @param cost
         */
        private void monitorResultLengthAndRt(Invoker<?> invoker, Invocation invocation, Result result, Long cost) {
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

            // 慢接口标识(>=50ms)
            String slow = "false";
            if (cost == null) {
                slow = "unknown";
            } else if (cost >= 50) {
                slow = "true";
            }

            MonitorPoint point = MonitorPoint
                    .monitorKey("isharpever.dubbo.monitor")
                    .addTag("app", this.getAppCode(invoker))
                    .addTag("ip", ip)
                    .addTag("interface", interfaceName)
                    .addTag("method", methodName)
                    .addTag("slow", slow)
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
}
