package com.isharpever.tool.rule.build;

import com.isharpever.tool.rule.OperatorEnum;
import com.isharpever.tool.rule.DataTypeEnum;
import com.isharpever.tool.rule.build.check.NumberValueChecker;
import com.isharpever.tool.rule.build.check.ValueChecker;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Contains_Number_StatementBuilder extends AbstractStatementBuilder {
    @Override
    protected OperatorEnum supportOperator() {
        return OperatorEnum.CONTAINS;
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
        StringBuilder sb = new StringBuilder();
        for (String one : value) {
            sb.append("ToolFunc.containsNumber(").append(buildFactor(field)).append(",\"").append(one).append("\") && ");
        }
        sb.delete(sb.lastIndexOf(" && "), sb.length());
        result.addCondition(sb);
        result.addImports("import com.isharpever.tool.rule.ToolFunc;");
        return result;
    }
}
