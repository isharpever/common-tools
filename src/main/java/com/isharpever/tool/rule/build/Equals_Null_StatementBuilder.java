package com.isharpever.tool.rule.build;

import com.isharpever.tool.rule.OperatorEnum;
import com.isharpever.tool.rule.DataTypeEnum;
import com.isharpever.tool.rule.build.check.DoNothingValueChecker;
import com.isharpever.tool.rule.build.check.ValueChecker;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Equals_Null_StatementBuilder extends AbstractStatementBuilder {
    @Override
    protected OperatorEnum supportOperator() {
        return OperatorEnum.EQUALS;
    }

    @Override
    protected DataTypeEnum supportDataType() {
        return DataTypeEnum.NULL;
    }

    @Override
    protected ValueChecker valueChecker() {
        return DoNothingValueChecker.INSTANCE;
    }

    @Override
    public StatementBuildResult doBuild(String field, List<String> value) {
        StatementBuildResult result = new StatementBuildResult();
        result.addCondition(buildFactor(field) + "==null");
        return result;
    }
}
