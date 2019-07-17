package com.isharpever.tool.mdc;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

public class LogUniqueKeyUtil {
    private static final String LOG_KEY = "unique";

    public LogUniqueKeyUtil() {
    }

    public static String generateKey() {
        return UniqueIdGenerator.generateRandomNumStr(16);
    }

    public static void generateKeyToLog() {
        generateKeyToLog(generateKey());
    }

    public static void generateKeyToLogIfAbsent() {
        if (StringUtils.isBlank(getKeyFromLog())) {
            generateKeyToLog(generateKey());
        }
    }

    public static void generateKeyToLog(String logKey) {
        if (StringUtils.isBlank(logKey)) {
            logKey = generateKey();
        }
        MDC.put(LOG_KEY, logKey);
    }

    public static void removeKeyFromLog() {
        MDC.remove(LOG_KEY);
    }

    public static String getKeyFromLog() {
        return MDC.get(LOG_KEY);
    }

}
