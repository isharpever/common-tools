package com.isharpever.tool.datasource.routing;

/**
 * 数据源类型
 */
public enum DbTypeEn {

    /** 默认 */
    DEFAULT("default")
    ;

    DbTypeEn(String mean) {
        this.mean = mean;
    }

    private String mean;

    public String getMean() {
        return mean;
    }
}
