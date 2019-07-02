package com.isharpever.tool.mq.consumer;

import com.dianwoba.monitor.client.MonitorFactory;
import com.dianwoba.monitor.client.MonitorPoint;
import com.dianwoba.monitor.client.MonitorUtil;
import com.dianwoba.monitor.client.annotation.Monitor;
import com.isharpever.tool.mdc.LogUniqueKeyUtil;
import com.isharpever.tool.mq.consumer.handler.MessageHandler;
import com.isharpever.tool.mq.consumer.handler.MessageHandlerFactory;
import com.isharpever.tool.utils.NetUtil;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;

/**
 * rocketmq消息监听基类
 *
 * @author yinxiaolin
 * @date 2019/1/22
 */
@Slf4j
public abstract class AbstractFilteredMessageListener implements MessageListenerConcurrently, CustomMessageFilter {

    private MonitorUtil monitor = MonitorFactory.connect();

    @Value("${app.code}")
    private String appCode;

    @Monitor
    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
            ConsumeConcurrentlyContext context) {
        if (CollectionUtils.isEmpty(msgs)) {
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }

        LogUniqueKeyUtil.generateKeyToLogIfAbsent();

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
            // 处理消息
            long start = System.currentTimeMillis();
            handler.handle(msg);

            // 监控埋点:处理耗时
            this.monitorRt(handler.getClass().getName(), System.currentTimeMillis() - start);
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

    /**
     * 监控埋点
     * @param handlerName handler类名
     * @param rt 处理耗时
     */
    private void monitorRt(String handlerName, long rt) {
        String ip = NetUtil.getLocalHostAddress();

        MonitorPoint point = MonitorPoint
                .monitorKey("isharpever.mq.handler")
                .addTag("app", this.appCode)
                .addTag("ip", ip)
                .addTag("handler", handlerName)
                .addField("rt", rt).build();
        monitor.writePoint(point);
    }
}
