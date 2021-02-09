package com.isharpever.tool.rule.build;

import com.isharpever.tool.rule.OperatorEnum;
import com.isharpever.tool.rule.ValueTypeEnum;
import com.isharpever.tool.rule.build.AbstractStatementBuilder;
import com.isharpever.tool.rule.build.StatementBuildResult;
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
    protected ValueTypeEnum supportValueType() {
        return ValueTypeEnum.NUMBER;
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
            sb.append(buildFactor(field)).append(".contains(").append(convertValue(one)).append(") && ");
        }
        sb.delete(sb.lastIndexOf(" && "), sb.length());
        result.addCondition(sb);
        return result;
    }

    private double convertValue(String value) {
        return Double.parseDouble(value);
    }

}
