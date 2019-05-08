package com.dianwoda.isharpever.tool.mdc;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

public class LogUniqueKeyUtil {
    public static String LOG_KEY = "unique";

    public LogUniqueKeyUtil() {
    }

    public static String generateKey() {
        return UniqueIdGenerator.generateRandomNumStr(16);
    }

    public static void generateKeyToLog() {
        MDC.put(LOG_KEY, generateKey());
    }

    public static void removeKeyFromLog() {
        MDC.remove(LOG_KEY);
    }

    public static String getKeyFromLog() {
        return MDC.get(LOG_KEY);
    }

    public static void setLogKeyIfNullAdd(String key) {
        String tmp = key;
        if (StringUtils.isEmpty(key)) {
            tmp = generateKey();
        }

        MDC.put(LOG_KEY, tmp);
    }
}
