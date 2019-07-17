package com.isharpever.tool.methodmonitor;

import com.dianwoba.monitor.client.MonitorPoint;
import com.dianwoba.monitor.client.MonitorUtil;
import com.dianwoba.monitor.client.MonitorUtilImpl;
import com.isharpever.tool.utils.AppNameUtil;
import com.isharpever.tool.utils.NetUtil;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Slf4j
public class MethodMonitorAspect {

    private MonitorUtil monitor = new MonitorUtilImpl();

    @Pointcut("@annotation(MethodMonitor)")
    public void pointcut() {}

    @Around("pointcut()")
    public Object arround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            // 调用目标方法
            return joinPoint.proceed();
        } finally {
            try {
                // 目标方法签名
                String[] target = this.getTargetMethodSignure(joinPoint);

                // 写入监控
                MonitorPoint point = MonitorPoint
                        .monitorKey("isharpever.method.monitor")
                        .addTag("app", AppNameUtil.getAppName())
                        .addTag("ip", NetUtil.getLocalHostAddress())
                        .addTag("class", target[0])
                        .addTag("method", target[1])
                        .addField("rt", System.currentTimeMillis() - startTime)
                        .build();
                monitor.writePoint(point);
            } catch (Exception e) {
                log.warn("--- 监控目标方式发生异常", e);
            }
        }
    }

    /**
     * 返回目标方法签名
     * @param pjp
     * @return
     */
    private String[] getTargetMethodSignure(ProceedingJoinPoint pjp) {
        String[] result = new String[]{"unknownclass", "unknownmethod"};
        try {
            if (pjp.getSignature() instanceof MethodSignature) {
                MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
                Method method = methodSignature.getMethod();
                result[0] = method.getDeclaringClass().getName();
                result[1] = method.getName();
            }
        } catch (Exception e) {
            log.warn("--- 获取目标方法签名失败", e);
        }
        return result;
    }
}
