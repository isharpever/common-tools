package com.isharpever.tool.rule.build;

import com.isharpever.tool.rule.OperatorEnum;
import com.isharpever.tool.rule.DataTypeEnum;
import com.isharpever.tool.rule.build.check.NumberValueChecker;
import com.isharpever.tool.rule.build.check.ValueChecker;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Equals_Number_StatementBuilder extends AbstractStatementBuilder {
    @Override
    protected OperatorEnum supportOperator() {
        return OperatorEnum.EQUALS;
    }

    @Override
    protected DataTypeEnum supportDataType() {
        return DataTypeEnum.NUMBER;
    }

    @Override
    protected ValueChecker valueChecker() {
        return NumberValueChecker.INSTANCE;
    }

    @Override
    public StatementBuildResult doBuild(String field, List<String> value) {
        StatementBuildResult result = new StatementBuildResult();
        StringBuilder sb = new StringBuilder("ToolFunc.compareNumber(")
                .append(buildFactor(field)).append(",\"").append(value.get(0)).append("\")==0");
        result.addCondition(sb);
        result.addImports("import com.isharpever.tool.rule.ToolFunc;");
        return result;
    }
}
