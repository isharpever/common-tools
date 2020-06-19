package com.isharpever.tool.redis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * 启用RedisTemplate bean
 *
 * @author yinxiaolin
 * @date 2020/03/13
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(RedisTemplateConfiguration.class)
public @interface EnableRedisTemplate {

}
