package com.isharpever.tool.rule.build;

import com.isharpever.tool.rule.OperatorEnum;
import com.isharpever.tool.rule.ValueTypeEnum;
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
        result.addCondition(buildFactor(field) + "==" + convertValue(value));
        return result;
    }

    private double convertValue(List<String> value) {
        return Double.parseDouble(value.get(0));
    }

}
