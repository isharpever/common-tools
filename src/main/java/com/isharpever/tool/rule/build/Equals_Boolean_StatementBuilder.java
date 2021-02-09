package com.isharpever.tool.rule.build;

import com.isharpever.tool.rule.OperatorEnum;
import com.isharpever.tool.rule.DataTypeEnum;
import com.isharpever.tool.rule.build.check.BoolValueChecker;
import com.isharpever.tool.rule.build.check.ValueChecker;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Equals_Boolean_StatementBuilder extends AbstractStatementBuilder {
    @Override
    protected OperatorEnum supportOperator() {
        return OperatorEnum.EQUALS;
    }

    @Override
    protected DataTypeEnum supportDataType() {
        return DataTypeEnum.BOOLEAN;
    }

    @Override
    protected ValueChecker valueChecker() {
        return BoolValueChecker.INSTANCE;
    }

    @Override
    public StatementBuildResult doBuild(String field, List<String> value) {
        StatementBuildResult result = new StatementBuildResult();
        result.addCondition((convertValue(value) ? "" : "!") + buildFactor(field));
        return result;
    }

    private boolean convertValue(List<String> value) {
        return Boolean.parseBoolean(value.get(0));
    }

}
