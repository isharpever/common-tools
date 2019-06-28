package com.isharpever.tool.utils;

import org.apache.commons.lang3.StringUtils;

public class AppNameUtil {

    /**
     * 返回应用名
     * @return
     */
    public static String getAppName() {
        String appName = System.getProperty("project.name");
        if (StringUtils.isNotBlank(appName)) {
            return appName;
        }
        return "unknown";
    }
}
