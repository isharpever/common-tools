package com.isharpever.tool.rule;

import com.alibaba.fastjson.JSON;
import com.isharpever.tool.rule.build.StatementBuildResult;
import com.isharpever.tool.rule.build.StatementBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class RuleParser {

    public static Rule parse(String conditionGroupText) {
        if (StringUtils.isEmpty(conditionGroupText)) {
            return null;
        }
        Rule result = new Rule();
        ConditionGroup conditionGroup = JSON.parseObject(conditionGroupText, ConditionGroup.class);
        StatementBuildResult buildResult = parseConditionGroup(conditionGroup);
        StringBuilder sourceCode = new StringBuilder();
        // import
        for (String anImport : buildResult.getImports()) {
            sourceCode.append(anImport).append("\n");
        }
        // 条件
        sourceCode.append("return ").append(buildResult.getCondition()).append(";");
        // 条件使用到的函数
        for (String function : buildResult.getFunctions()) {
            sourceCode.append("\n").append(function);
        }
        result.setSourceCode(sourceCode.toString());
        result.setFactors(new ArrayList<>(buildResult.getFactors()));
        return result;
    }

    private static StatementBuildResult parseConditionGroup(ConditionGroup conditionGroup) {
        StatementBuildResult result = new StatementBuildResult();

        // 规则
        if (CollectionUtils.isNotEmpty(conditionGroup.getConditions())) {
            for (Condition condition : conditionGroup.getConditions()) {
                StatementBuildResult conditionResult = StatementBuilderFactory.buildStatement(condition.getField(), condition.getOperator(), condition.getValue(), condition.getValueType());
                result.mergeFrom(conditionResult);
                joint(result.getCondition(), conditionGroup.getCojunction());
            }
        }

        // 规则组
        if (CollectionUtils.isNotEmpty(conditionGroup.getConditionGroups())) {
            for (ConditionGroup group : conditionGroup.getConditionGroups()) {
                StatementBuildResult groupResult = parseConditionGroup(group);
                result.mergeFrom(groupResult);
                joint(result.getCondition(), conditionGroup.getCojunction());
            }
        }

        result.getCondition().delete(result.getCondition().lastIndexOf(conditionGroup.getCojunction()) - 1, result.getCondition().length());
        result.getCondition().insert(0, conditionGroup.isNot() ? "!(" : "(").append(")");
        return result;
    }

    private static void joint(StringBuilder sb, String conjunction) {
        sb.append(" ").append(conjunction).append("\n");
    }
}
