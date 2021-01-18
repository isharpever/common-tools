package com.isharpever.tool.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class NetUtil {

    /**
     * 本机ip
     */
    private volatile static String localHostAddress;

    /**
     * 返回本机ip
     * @return
     */
    public static String getLocalHostAddress() {
        if (StringUtils.isNotBlank(localHostAddress)) {
            return localHostAddress;
        }
        try {
            localHostAddress = InetAddress.getLocalHost().getHostAddress();
            return localHostAddress;
        } catch (UnknownHostException e) {
            log.error("获取本地ip异常", e);
            return "unknown host";
        }
    }
}
