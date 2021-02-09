package com.isharpever.tool.rule.build.check;

import com.isharpever.tool.rule.ValueTypeEnum;

import java.util.List;

public class DateTimeValueChecker implements ValueChecker {
    public static final DateTimeValueChecker INSTANCE = new DateTimeValueChecker();

    private DateTimeValueChecker() {}

    @Override
    public void check(List<String> value) throws ValueCheckException {
        NotEmptyValueChecker.INSTANCE.check(value);
        for (String one : value) {
            ValueTypeEnum.DATETIME.formatCheck(one);
        }
    }
}
