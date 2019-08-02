package com.isharpever.tool.mq.consumer;

import com.isharpever.tool.mdc.LogUniqueKeyUtil;
import com.isharpever.tool.methodmonitor.MethodMonitor;
import com.isharpever.tool.mq.consumer.handler.MessageHandler;
import com.isharpever.tool.mq.consumer.handler.MessageHandlerFactory;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * rocketmq消息监听基类
 *
 * @author yinxiaolin
 * @date 2019/1/22
 */
@Slf4j
public abstract class AbstractFilteredMessageListener implements MessageListenerConcurrently, CustomMessageFilter {

    @MethodMonitor
    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
            ConsumeConcurrentlyContext context) {
        if (CollectionUtils.isEmpty(msgs)) {
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }

        LogUniqueKeyUtil.generateKeyToLog();

        MessageExt msg = msgs.get(0);
        String body = new String(msg.getBody());
        String tags = msg.getTags();
        String topic = msg.getTopic();
        log.info("{} 收到新消息 {}，key：{}，tags：{}，topics：{}",
                Thread.currentThread().getName(), body, msg.getKeys(), tags, topic);

        // 消息过滤
        if (!this.accept(msg)) {
            return this.onDeny(msg);
        }

        ConsumeConcurrentlyStatus consumeConcurrentlyStatus = ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

        try {
            MessageHandler handler = MessageHandlerFactory.resolve(topic, tags);
            if (handler == null) {
                return consumeConcurrentlyStatus;
            }
            handler.handle(msg);
        } catch (Exception e) {
            log.error("消息消费出现异常 messageBody={}", body, e);
            consumeConcurrentlyStatus = ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }

        return consumeConcurrentlyStatus;
    }

    /**
     * 消息被拒绝时的处理
     * @param messageExt
     * @return
     */
    @Override
    public ConsumeConcurrentlyStatus onDeny(MessageExt messageExt) {
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
}
