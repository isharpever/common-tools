package com.isharpever.tool.rule.build.check;

import com.isharpever.tool.rule.ValueTypeEnum;

import java.util.List;

public class NumberValueChecker implements ValueChecker {
    public static final NumberValueChecker INSTANCE = new NumberValueChecker();

    private NumberValueChecker() {}

    @Override
    public void check(List<String> value) throws ValueCheckException {
        NotEmptyValueChecker.INSTANCE.check(value);
        for (String one : value) {
            ValueTypeEnum.NUMBER.formatCheck(one);
        }
    }
}
