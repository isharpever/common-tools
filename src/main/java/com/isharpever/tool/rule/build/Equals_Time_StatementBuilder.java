package com.isharpever.tool.rule.build;

import com.isharpever.tool.rule.OperatorEnum;
import com.isharpever.tool.rule.ValueTypeEnum;
import com.isharpever.tool.rule.build.check.TimeValueChecker;
import com.isharpever.tool.rule.build.check.ValueChecker;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Equals_Time_StatementBuilder extends AbstractStatementBuilder {
    @Override
    protected OperatorEnum supportOperator() {
        return OperatorEnum.EQUALS;
    }

    @Override
    protected ValueTypeEnum supportValueType() {
        return ValueTypeEnum.TIME;
    }

    @Override
    protected ValueChecker valueChecker() {
        return TimeValueChecker.INSTANCE;
    }

    @Override
    public StatementBuildResult doBuild(String field, List<String> value) {
        StatementBuildResult result = new StatementBuildResult();
        result.addCondition("formatHms(" + buildFactor(field) + ").equals(\"" + value.get(0) + "\")");
        result.addFunctions(formatHmsFunc());
        result.addImports("import java.text.SimpleDateFormat;");
        return result;
    }

    private String formatHmsFunc() {
        return "private String formatHms(Date date) {\n" +
                "    SimpleDateFormat format = new SimpleDateFormat(\"HH:mm:ss\");\n" +
                "    return format.format(date);\n" +
                "}";
    }
}
