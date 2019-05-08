package com.dianwoda.isharpever.tool.mdc;

import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniqueIdGenerator {
    private static Logger logger = LoggerFactory.getLogger(UniqueIdGenerator.class);
    private static SecureRandom secureRandom;
    private static char[] charList;
    private static char[] numberList;

    public UniqueIdGenerator() {
    }

    public static long genUniqueId() {
        long result = System.nanoTime();
        if (secureRandom != null) {
            long randomLong = secureRandom.nextLong();
            result = Math.abs(randomLong);
        }

        return result;
    }

    public static String generateRandomStr(int length) {
        return generateRandomNumStr(length);
    }

    public static String generateRandomNumStr(int length) {
        char[] result = new char[length];
        long random = genUniqueId();

        for(int i = 0; i < length; ++i) {
            int idx = (int)(random % 100L % (long)numberList.length);
            result[i] = numberList[idx];
            random /= 10L;
            if (random == 0L) {
                random = genUniqueId();
            }
        }

        return new String(result);
    }

    static {
        String lowerStr = "abcdefghijklmnopqrstuvwxyz";
        String upperStr = lowerStr.toUpperCase();
        String numberStr = "1234567890";
        String s = lowerStr + upperStr + numberStr;
        charList = s.toCharArray();
        numberList = numberStr.toCharArray();

        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (Exception var5) {
            logger.error("error in UniqueIdGenerator:secureRandom", var5);
        }

    }
}
