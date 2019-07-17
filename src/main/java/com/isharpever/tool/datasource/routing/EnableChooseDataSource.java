package com.isharpever.tool.datasource.routing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * 配置切面,开启@DataSource切换数据源功能
 *
 * @author yinxiaolin
 * @since 2019/7/10
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Import(DataSourceAspect.class)
public @interface EnableChooseDataSource {
}
