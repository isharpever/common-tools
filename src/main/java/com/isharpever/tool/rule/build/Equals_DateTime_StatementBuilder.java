package com.isharpever.tool.rule.build;

import com.isharpever.tool.rule.OperatorEnum;
import com.isharpever.tool.rule.DataTypeEnum;
import com.isharpever.tool.rule.build.check.DateTimeValueChecker;
import com.isharpever.tool.rule.build.check.ValueChecker;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Equals_DateTime_StatementBuilder extends AbstractStatementBuilder {
    @Override
    protected OperatorEnum supportOperator() {
        return OperatorEnum.EQUALS;
    }

    @Override
    protected DataTypeEnum supportDataType() {
        return DataTypeEnum.DATETIME;
    }

    @Override
    protected ValueChecker valueChecker() {
        return DateTimeValueChecker.INSTANCE;
    }

    @Override
    public StatementBuildResult doBuild(String field, List<String> value) {
        StatementBuildResult result = new StatementBuildResult();
        result.addCondition("ToolFunc.compareYmdHms(" + buildFactor(field) + ",\"" + value.get(0) + "\")==0");
        result.addImports("import com.isharpever.tool.rule.ToolFunc;");
        return result;
    }
}
