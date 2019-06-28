package com.isharpever.tool.utils;

import com.dianwoba.core.exception.BusinessException;
import com.isharpever.tool.enums.CustomLogLevel;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;

/**
 * rocketmq相关
 *
 * @author yinxiaolin
 * @since 2019/6/14
 */
@Slf4j
public class RocketMQUtil {
    private static final Logger logger = LogManager.getLogger(RocketMQUtil.class);

    /**
     * 发送+重试
     * @param msg 待发送消息
     * @param maxSendTimes 最大尝试发送次数
     * @param producer 消息发送者
     */
    public static void sendWithRetry(DefaultMQProducer producer, Message msg, int maxSendTimes) {
        if (producer == null) {
            throw new BusinessException("producer必传");
        }
        int sendTimes = 0;
        SendResult sendResult = null;
        while (!success(sendResult) && sendTimes < maxSendTimes) {
            // 先等待再重试
            try {
                TimeUnit.SECONDS.sleep(sendTimes * 5);
            } catch (InterruptedException e) {
            }
            try {
                sendResult = producer.send(msg);
            } catch (Exception e) {
                logger.log(CustomLogLevel.DING.toLevel(), "--- 消息发送失败,稍后重试 msg={}", msg, e);
            }
            sendTimes++;
        }

        if (!success(sendResult)) {
            log.error("--- 消息发送失败,重试已达上限 msg={}", msg);
        }
    }

    /**
     * 判断发送结果是否成功
     * @param sendResult 发送结果
     * @return
     */
    public static boolean success(SendResult sendResult) {
        return sendResult != null && SendStatus.SEND_OK.equals(sendResult.getSendStatus());
    }
}
