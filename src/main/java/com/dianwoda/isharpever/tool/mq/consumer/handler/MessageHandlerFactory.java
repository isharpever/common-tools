package com.dianwoda.isharpever.tool.mq.consumer.handler;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * MessageHandler工厂
 *
 * @author yinxiaolin
 * @since 2019/1/11
 */
@Slf4j
public class MessageHandlerFactory {

    private static final List<MessageHandler> ALL_HANDLER = Lists.newArrayList();

    /**
     * 注册MessageHandler
     * @param handler
     */
    public static void register(MessageHandler handler) {
        ALL_HANDLER.add(handler);
    }

    /**
     * 返回能处理指定类型消息的MessageHandler
     * @param topic
     * @param tag
     * @return
     */
    public static MessageHandler resolve(String topic, String tag) {
        for (MessageHandler handler : ALL_HANDLER) {
            if (handler.support(topic, tag)) {
                return handler;
            }
        }
        log.warn("--- 未找到能处理此类型消息的MessageHandler topic={} tag={}", topic, tag);
        return null;
    }
}
