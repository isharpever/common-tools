package com.isharpever.tool.datasource.routing;


import java.util.Stack;

/**
 * 线程本地缓存当前数据源的lookupKey
 */
public class DataSourceLookupKeyHolder {

    private static final ThreadLocal<Stack<Object>> lookupKey = ThreadLocal.withInitial(Stack::new);

    public static void put(Object dataSourceLookupKey) {
        lookupKey.get().push(dataSourceLookupKey);
    }

    public static Object get() {
        if (lookupKey.get().empty()) {
            return null;
        }
        return lookupKey.get().peek();
    }

    public static void pop() {
        if (!lookupKey.get().empty()) {
            lookupKey.get().pop();
        }
    }
}
