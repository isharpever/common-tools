package com.isharpever.tool.datasource.monitor;

import java.util.ArrayList;
import java.util.List;

/**
 * 存储慢查询sql的参数
 *
 * @author yinxiaolin
 * @date 2018/11/18
 */
public class ParamHolder {
    public static ThreadLocal<List<Object>> params = ThreadLocal.withInitial(ArrayList::new);

    public ParamHolder() {
    }
}