package com.isharpever.tool.rule.build.check;

import com.isharpever.tool.rule.DataTypeEnum;

import java.util.List;

public class TimeValueChecker implements ValueChecker {
    public static final TimeValueChecker INSTANCE = new TimeValueChecker();

    private TimeValueChecker() {}

    @Override
    public void check(List<String> value) throws ValueCheckException {
        NotEmptyValueChecker.INSTANCE.check(value);
        for (String one : value) {
            DataTypeEnum.TIME.formatCheck(one);
        }
    }
}
