package com.isharpever.tool.mq.consumer.handler;

import javax.annotation.PostConstruct;

/**
 * 消息处理handler基类
 */
public abstract class AbstractMessageHandler implements MessageHandler {

    @PostConstruct
    public void init() {
        MessageHandlerFactory.register(this);
    }
}
