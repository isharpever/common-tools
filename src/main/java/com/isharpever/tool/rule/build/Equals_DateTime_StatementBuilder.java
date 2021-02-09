package com.isharpever.tool.rule.build;

import com.isharpever.tool.rule.OperatorEnum;
import com.isharpever.tool.rule.ValueTypeEnum;
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
    protected ValueTypeEnum supportValueType() {
        return ValueTypeEnum.DATETIME;
    }

    @Override
    protected ValueChecker valueChecker() {
        return DateTimeValueChecker.INSTANCE;
    }

    @Override
    public StatementBuildResult doBuild(String field, List<String> value) {
        StatementBuildResult result = new StatementBuildResult();
        result.addCondition("formatYmdHms(" + buildFactor(field) + ").equals(\"" + value.get(0) + "\")");
        result.addFunctions(formatYmdHmsFunc());
        result.addImports("import java.text.SimpleDateFormat;");
        return result;
    }

    private String formatYmdHmsFunc() {
        return "private String formatYmdHms(Date date) {\n" +
                "    SimpleDateFormat format = new SimpleDateFormat(\"yyyy-MM-dd HH:mm:ss\");\n" +
                "    return format.format(date);\n" +
                "}";
    }
}
