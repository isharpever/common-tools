package com.isharpever.tool.rule;

public enum OperatorEnum {
    EQUALS("=="),
    NOT_EQUALS("!="),
    GT(">"),
    GTE(">="),
    LT("<"),
    LTE("<="),
    START_WITH("以此开头"),
    END_WITH("以此结尾"),
    MATCHES("匹配"),
    CONTAINS("包含"),
    NOT_CONTAINS("不包含")
    ;

    private final String operator;

    OperatorEnum(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return this.operator;
    }
}
