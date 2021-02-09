package com.isharpever.tool.rule.build;

import com.isharpever.tool.rule.Condition;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class StatementBuilderFactory {
    public static final Map<String, StatementBuilder> builders = new HashMap<>();

    public static void registerStatementBuilder(StatementBuilder statementBuilder) {
        builders.put(statementBuilder.identity(), statementBuilder);
    }

    public static StatementBuildResult buildStatement(Condition condition) {
        String dataType = condition.getValueType();
        if ("const".equals(dataType)) {
            dataType = condition.getFieldType();
        }
        String identity = String.format(StatementBuilder.IDENTITY_FORMAT, condition.getOperator(), dataType);
        StatementBuilder statementBuilder = builders.get(identity);
        if (statementBuilder == null) {
            log.error("无法处理的操作符或数据类型 operator='{}' dataType='{}'", condition.getOperator(), dataType);
            throw new IllegalArgumentException("无法处理的操作符或数据类型");
        }
        return statementBuilder.build(condition.getField(), condition.getValue());
    }
}
