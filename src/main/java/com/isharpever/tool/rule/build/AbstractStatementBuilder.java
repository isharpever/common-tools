package com.isharpever.tool.rule.build;

import com.isharpever.tool.rule.OperatorEnum;
import com.isharpever.tool.rule.ValueTypeEnum;
import com.isharpever.tool.rule.build.check.ValueChecker;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public abstract class AbstractStatementBuilder implements StatementBuilder {
    public AbstractStatementBuilder() {
        register();
    }

    @Override
    public String identity() {
        return String.format(IDENTITY_FORMAT, supportOperator().getOperator(), supportValueType().getValueType());
    }

    @Override
    public void register() {
        StatementBuilderFactory.registerStatementBuilder(this);
    }

    @Override
    public StatementBuildResult build(String field, List<String> value) {
        if (StringUtils.isEmpty(field)) {
            throw new IllegalArgumentException("field不能为空");
        }
        doCheck(field, value);
        StatementBuildResult result = doBuild(field, value);
        result.addFactors(field);
        result.getCondition().insert(0, "(").append(")");
        return result;
    }

    protected void doCheck(String field, List<String> value) {
        valueChecker().check(value);
    };

    protected String buildFactor(String factor) {
        StringBuilder sb = new StringBuilder();
        sb.append(StatementBuilder.PARAMS_VARIABLE).append(".get(\"").append(factor).append("\")");
        return sb.toString();
    }

    protected abstract OperatorEnum supportOperator();
    protected abstract ValueTypeEnum supportValueType();
    protected abstract ValueChecker valueChecker();
    protected abstract StatementBuildResult doBuild(String field, List<String> value);
}
