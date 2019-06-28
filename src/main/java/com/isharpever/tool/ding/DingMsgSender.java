package com.isharpever.tool.ding;

import com.alibaba.fastjson.JSON;
import com.isharpever.tool.ding.vo.DingMsgVO;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 钉钉群消息相关
 *
 * @author yinxiaolin
 * @since 2019/1/10
 */
public class DingMsgSender {
    private static final Logger logger = LoggerFactory.getLogger(DingMsgSender.class);

    /**
     * 钉钉webhook请求最大重试次数(默认值)
     */
    private static final int DEFAULT_MAX_RETRIES = 2;

    /**
     * httpclient
     */
    private CustomAsyncHttpClient customAsyncHttpClient;

    /**
     * 钉钉webhook请求最大重试次数
     */
    private int maxRetries = DEFAULT_MAX_RETRIES;

    public DingMsgSender() {
        this.customAsyncHttpClient = CustomAsyncHttpClient.createBuilder().build();
    }

    public DingMsgSender(CustomAsyncHttpClient customAsyncHttpClient) {
        if (customAsyncHttpClient != null) {
            this.customAsyncHttpClient = customAsyncHttpClient;
        } else {
            logger.warn("--- 构造参数为null,创建默认的httpclient");
            this.customAsyncHttpClient = CustomAsyncHttpClient.createBuilder().build();
        }
    }

    public DingMsgSender(CustomAsyncHttpClient customAsyncHttpClient, int retries) {
        if (customAsyncHttpClient != null) {
            this.customAsyncHttpClient = customAsyncHttpClient;
        } else {
            logger.warn("--- 构造参数为null,创建默认的httpclient");
            this.customAsyncHttpClient = CustomAsyncHttpClient.createBuilder().build();
        }

        this.maxRetries = retries;
    }

    /**
     * 发送钉钉消息
     * @param webhook
     * @param msgBody
     */
    public void sendDingMsg(String webhook, String msgBody) {
        this.sendDingMsg(webhook, msgBody, null, maxRetries);
    }

    /**
     * 发送钉钉消息
     * @param webhook
     * @param msgBody
     * @param atMobiles
     */
    public void sendDingMsg(String webhook, String msgBody, String atMobiles) {
        this.sendDingMsg(webhook, msgBody, atMobiles, maxRetries);
    }

    /**
     * 发送钉钉消息
     * @param webhook
     * @param msgBody
     * @param atMobiles
     * @param maxRetries
     */
    public void sendDingMsg(String webhook, String msgBody, String atMobiles, int maxRetries) {
        if (customAsyncHttpClient == null) {
            logger.warn("--- httpclient未正确初始化 webhook={} msgBody={}", webhook, msgBody);
            return;
        }
        if (StringUtils.isBlank(webhook)) {
            logger.info("--- 未指定webhook msgBody={}", msgBody);
            return;
        }

        // 构建钉钉消息
        DingMsgVO dingMsgVO = this.buildDingMsgVO(msgBody, atMobiles);
        String msg = JSON.toJSONString(dingMsgVO);

        // 发送及发送结果处理
        HttpUriRequest request = this.buildHttpRequest(webhook, msg);
        FutureCallback<HttpResponse> callback =
                new CustomFutureCallback(customAsyncHttpClient, webhook, msg, request, maxRetries);
        customAsyncHttpClient.execute(request, callback);
    }

    /**
     * 构建钉钉消息体
     * @param msgBody
     * @param atMobiles
     * @return
     */
    private DingMsgVO buildDingMsgVO(String msgBody, String atMobiles) {
        return DingMsgVO.newBuilder()
                .setMsgBody(msgBody)
                .setAtMobiles(atMobiles)
                .build();
    }

    /**
     * 构建钉钉webhook请求
     * @param webhook
     * @param msg
     * @return
     */
    private HttpUriRequest buildHttpRequest(String webhook, String msg) {
        HttpPost post = new HttpPost(webhook);
        post.addHeader("Content-Type", "application/json; charset=utf-8");
        StringEntity entity = new StringEntity(msg, "utf-8");
        post.setEntity(entity);
        return post;
    }

    /**
     * 钉钉webhook请求回调处理
     */
    private static class CustomFutureCallback implements FutureCallback<HttpResponse> {

        /**
         * httpclient
         */
        private CustomAsyncHttpClient customAsyncHttpClient;

        /**
         * 请求最大重试次数
         */
        private int MAX_RETRY = DingMsgSender.DEFAULT_MAX_RETRIES;

        /**
         * webhook
         */
        private String webhook;

        /**
         * 钉钉消息
         */
        private String msg;

        /**
         * 钉钉webhook请求
         */
        private final HttpUriRequest request;

        /**
         * 请求重试次数
         */
        private int retries;

        public CustomFutureCallback(CustomAsyncHttpClient customAsyncHttpClient,
                String webhook, String msg, HttpUriRequest request, int retries) {
            this.customAsyncHttpClient = customAsyncHttpClient;
            this.webhook = webhook;
            this.msg = msg;
            this.request = request;
            this.MAX_RETRY = retries;
        }

        @Override
        public void completed(HttpResponse result) {
            if (result == null || result.getStatusLine() == null
                    || result.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                retries++;
                if (retries <= MAX_RETRY) {
                    customAsyncHttpClient.execute(request, this);
                } else {
                    logger.warn("--- 钉钉消息发送失败,响应不符合预期 webhook={} msg={} retries={}",
                            webhook, msg, retries);
                }
            } else {
                logger.info("--- 钉钉消息发送成功 webhook={} msg={}", webhook, msg);
                try {
                    logger.info("--- response={}",
                            EntityUtils.toString(result.getEntity(), "utf-8"));
                } catch (IOException e) {
                }
            }
        }

        @Override
        public void failed(Exception ex) {
            retries++;
            if (retries <= MAX_RETRY) {
                customAsyncHttpClient.execute(request, this);
            } else {
                logger.warn("--- 钉钉消息发送失败 webhook={} msg={} retries={}",
                        webhook, msg, retries, ex);
            }
        }

        @Override
        public void cancelled() {
            retries++;
            if (retries <= MAX_RETRY) {
                customAsyncHttpClient.execute(request, this);
            } else {
                logger.warn("--- 钉钉消息被取消发送 webhook={} msg={} retries={}",
                        webhook, msg, retries);
            }
        }
    }
}
