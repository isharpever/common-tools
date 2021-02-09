package com.isharpever.tool.rule.build;

import com.isharpever.tool.rule.OperatorEnum;
import com.isharpever.tool.rule.ValueTypeEnum;
import com.isharpever.tool.rule.build.check.NotEmptyValueChecker;
import com.isharpever.tool.rule.build.check.ValueChecker;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Equals_Text_StatementBuilder extends AbstractStatementBuilder {
    @Override
    protected OperatorEnum supportOperator() {
        return OperatorEnum.EQUALS;
    }

    @Override
    protected ValueTypeEnum supportValueType() {
        return ValueTypeEnum.TEXT;
    }

    @Override
    protected ValueChecker valueChecker() {
        return NotEmptyValueChecker.INSTANCE;
    }

    @Override
    public StatementBuildResult doBuild(String field, List<String> value) {
        StatementBuildResult result = new StatementBuildResult();
        result.addCondition("Objects.equals(" + buildFactor(field) + ",\"" + value.get(0) + "\")");
        return result;
    }
}
