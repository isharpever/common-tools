package com.isharpever.tool.rule.build;

import com.isharpever.tool.rule.OperatorEnum;
import com.isharpever.tool.rule.DataTypeEnum;
import com.isharpever.tool.rule.build.check.TextValueChecker;
import com.isharpever.tool.rule.build.check.ValueChecker;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class StartWith_Text_StatementBuilder extends AbstractStatementBuilder {
    @Override
    protected OperatorEnum supportOperator() {
        return OperatorEnum.START_WITH;
    }

    @Override
    protected DataTypeEnum supportDataType() {
        return DataTypeEnum.TEXT;
    }

    @Override
    protected ValueChecker valueChecker() {
        return TextValueChecker.INSTANCE;
    }

    @Override
    public StatementBuildResult doBuild(String field, List<String> value) {
        StatementBuildResult result = new StatementBuildResult();
        StringBuilder sb = new StringBuilder();
        sb.append(buildFactor(field)).append(".startsWith(\"").append(value.get(0)).append("\")");
        result.addCondition(sb);
        return result;
    }
}
