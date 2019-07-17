package com.isharpever.tool.methodmonitor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * 配置切面,开启@MethodMonitor监控功能
 *
 * @author yinxiaolin
 * @since 2018/10/29
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Import(MethodMonitorAspect.class)
public @interface EnableMethodMonitor {
}
