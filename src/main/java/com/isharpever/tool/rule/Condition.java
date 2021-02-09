package com.isharpever.tool.rule;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class Condition {
    /**
     * 条件变量名(对应因子脚本名)
     */
    private String field;
    /**
     * 条件变量的数据类型(因子脚本执行结果的数据类型),集合/数组的话,其元素的数据类型
     * number/boolean/text/date/datatime/time
     */
    private String fieldType;
    private String operator;
    private List<String> value;
    /**
     * 条件值的类型
     * const/factor/null
     */
    private String valueType;
}
