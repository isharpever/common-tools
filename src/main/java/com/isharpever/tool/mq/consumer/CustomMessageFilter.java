package com.isharpever.tool.mq.consumer;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * 过滤mq消息
 */
interface CustomMessageFilter {

    /**
     * 返回是否接受此消息
     * @param messageExt
     * @return
     */
    boolean accept(MessageExt messageExt);

    /**
     * 消息被拒绝时的处理
     * @param messageExt
     * @return
     */
    ConsumeConcurrentlyStatus onDeny(MessageExt messageExt);
}
