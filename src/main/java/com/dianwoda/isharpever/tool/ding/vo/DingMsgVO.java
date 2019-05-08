package com.dianwoda.isharpever.tool.ding.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 钉钉消息结构
 *
 * @author yinxiaolin
 * @since 2018/6/1
 */
public class DingMsgVO {

    /**
     * 钉钉消息类型:固定为text
     */
    private String msgtype = "text";

    /**
     * 钉钉消息内容
     */
    private DingMsgTextVO text;

    private DingMsgAtVO at;

    public DingMsgVO() {}

    public DingMsgVO(DingMsgTextVO text, DingMsgAtVO at) {
        this.text = text;
        this.at = at;
    }

    public String getMsgtype() {
        return msgtype;
    }

    public void setMsgtype(String msgtype) {
        this.msgtype = msgtype;
    }

    public DingMsgTextVO getText() {
        return text;
    }

    public void setText(DingMsgTextVO text) {
        this.text = text;
    }

    public DingMsgAtVO getAt() {
        return at;
    }

    public void setAt(DingMsgAtVO at) {
        this.at = at;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        /**
         * 消息正文
         */
        private String msgBody;

        /**
         * 消息@人手机号
         */
        private String atMobiles;

        private Builder() {

        }

        public Builder setMsgBody(String msgBody) {
            this.msgBody = msgBody;
            return this;
        }

        public Builder setAtMobiles(String atMobiles) {
            this.atMobiles = atMobiles;
            return this;
        }

        public DingMsgVO build() {
            DingMsgTextVO dingMsgTextVO = new DingMsgTextVO(msgBody);
            DingMsgAtVO dingMsgAtVO = new DingMsgAtVO(atMobiles);
            return new DingMsgVO(dingMsgTextVO, dingMsgAtVO);
        }
    }

    /**
     * 钉钉消息@人结构
     */
    private static class DingMsgAtVO {

        /**
         * 钉钉消息被@人的手机号
         */
        private List<String> atMobiles;

        /**
         * 是否@所有人
         */
        @JSONField(name = "isAtAll")
        private Boolean atAll;

        public DingMsgAtVO() {
            this.resolveAtAll();
        }

        public DingMsgAtVO(List<String> atMobiles) {
            this.addMobiles(atMobiles);
            this.resolveAtAll();
        }

        public DingMsgAtVO(String atMobiles) {
            this.addMobiles(atMobiles);
            this.resolveAtAll();
        }

        public List<String> getAtMobiles() {
            return atMobiles;
        }

        public void setAtMobiles(List<String> atMobiles) {
            this.atMobiles = atMobiles;
            this.resolveAtAll();
        }

        public Boolean getAtAll() {
            return atAll;
        }

        public void setAtAll(Boolean atAll) {
            this.atAll = atAll;
        }

        /**
         * 添加@人
         */
        public void addMobiles(List<String> atMobiles) {
            if (CollectionUtils.isNotEmpty(atMobiles)) {
                if (CollectionUtils.isEmpty(this.atMobiles)) {
                    this.atMobiles = Lists.newArrayList();
                }
                this.atMobiles.addAll(atMobiles);
            }
        }

        /**
         * 添加@人
         */
        public void addMobiles(String atMobiles) {
            if (StringUtils.isNotBlank(atMobiles)) {
                List<String> mobiles = Arrays.asList(StringUtils.split(atMobiles, ","));
                this.addMobiles(mobiles);
            }
        }

        /**
         * 设置atAll
         */
        public void resolveAtAll() {
            this.setAtAll(CollectionUtils.isEmpty(this.atMobiles));
        }
    }

    /**
     * 钉钉消息内容结构
     */
    private static class DingMsgTextVO {

        /**
         * 钉钉消息内容
         */
        private String content;

        public DingMsgTextVO() {}

        public DingMsgTextVO(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
