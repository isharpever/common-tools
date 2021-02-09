package com.isharpever.tool.rule.build.check;

import com.isharpever.tool.rule.DataTypeEnum;

import java.util.List;

public class BoolValueChecker implements ValueChecker {
    public static final BoolValueChecker INSTANCE = new BoolValueChecker();

    private BoolValueChecker() {}

    @Override
    public void check(List<String> value) throws ValueCheckException {
        NotEmptyValueChecker.INSTANCE.check(value);
        for (String one : value) {
            DataTypeEnum.BOOLEAN.formatCheck(one);
        }
    }
}
