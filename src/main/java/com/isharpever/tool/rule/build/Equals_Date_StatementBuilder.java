package com.isharpever.tool.rule.build;

import com.isharpever.tool.rule.OperatorEnum;
import com.isharpever.tool.rule.ValueTypeEnum;
import com.isharpever.tool.rule.build.check.DateValueChecker;
import com.isharpever.tool.rule.build.check.ValueChecker;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Equals_Date_StatementBuilder extends AbstractStatementBuilder {
    @Override
    protected OperatorEnum supportOperator() {
        return OperatorEnum.EQUALS;
    }

    @Override
    protected ValueTypeEnum supportValueType() {
        return ValueTypeEnum.DATE;
    }

    @Override
    protected ValueChecker valueChecker() {
        return DateValueChecker.INSTANCE;
    }

    @Override
    public StatementBuildResult doBuild(String field, List<String> value) {
        StatementBuildResult result = new StatementBuildResult();
        result.addCondition("formatYmd(" + buildFactor(field) + ").equals(\"" + value.get(0) + "\")");
        result.addFunctions(formatYmdFunc());
        result.addImports("import java.text.SimpleDateFormat;");
        return result;
    }

    private String formatYmdFunc() {
        return "private String formatYmd(Date date) {\n" +
                "    SimpleDateFormat format = new SimpleDateFormat(\"yyyy-MM-dd\");\n" +
                "    return format.format(date);\n" +
                "}";
    }
}
