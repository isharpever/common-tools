package com.isharpever.tool.enums;

import org.apache.logging.log4j.Level;

/**
 * 自定义日志级别
 *
 * @author yinxiaolin
 * @since 2018/11/22
 */
public enum CustomLogLevel {
    /** 发送钉钉的日志级别 */
    DING("DING", 250)
    ;

    private String levelName;
    private int intLevel;

    CustomLogLevel(String levelName, int intLevel) {
        this.levelName = levelName;
        this.intLevel = intLevel;
    }

    public String getLevelName() {
        return levelName;
    }

    public int getIntLevel() {
        return intLevel;
    }

    public Level toLevel() {
        return Level.forName(this.getLevelName(), this.getIntLevel());
    }
}
