package com.isharpever.tool.methodmonitor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在方法上面,标识这个方法被纳入监控
 *
 * @author yinxiaolin
 * @since 2018/7/9
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface MethodMonitor {
}
