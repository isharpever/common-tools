package com.isharpever.tool.rule.build.check;

import com.isharpever.tool.rule.ValueTypeEnum;

import java.util.List;

public class DateValueChecker implements ValueChecker {
    public static final DateValueChecker INSTANCE = new DateValueChecker();

    private DateValueChecker() {}

    @Override
    public void check(List<String> value) throws ValueCheckException {
        NotEmptyValueChecker.INSTANCE.check(value);
        for (String one : value) {
            ValueTypeEnum.DATE.formatCheck(one);
        }
    }
}
