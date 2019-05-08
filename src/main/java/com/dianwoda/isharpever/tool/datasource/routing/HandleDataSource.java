package com.dianwoda.isharpever.tool.datasource.routing;


/**
 * 线程本地缓存当前数据源类型
 */
public class HandleDataSource {

    public static final ThreadLocal<String> holder = new ThreadLocal<>();
    public static void putDataSource(String datasource) {
        holder.set(datasource);
    }

    public static String getDataSource() {
        return holder.get();
    }

    public static void clear() {
        holder.remove();
    }
}
