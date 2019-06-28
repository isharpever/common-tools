package com.isharpever.tool.mq.consumer.handler;

import org.apache.rocketmq.common.message.MessageExt;

/**
 * 消息处理handler接口
 *
 * @author yinxiaolin
 * @since 2018/8/27
 */
public interface MessageHandler {

    /**
     * 返回是否能处理指定topic和tag的消息
     * @param topic
     * @param tag
     * @return
     */
    boolean support(String topic, String tag);

    /**
     * 处理指定消息
     * @param messageExt
     */
    void handle(MessageExt messageExt);
}
