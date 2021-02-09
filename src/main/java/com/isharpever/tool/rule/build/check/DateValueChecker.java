package com.isharpever.tool.rule.build.check;

import com.isharpever.tool.rule.DataTypeEnum;

import java.util.List;

public class DateValueChecker implements ValueChecker {
    public static final DateValueChecker INSTANCE = new DateValueChecker();

    private DateValueChecker() {}

    @Override
    public void check(List<String> value) throws ValueCheckException {
        NotEmptyValueChecker.INSTANCE.check(value);
        for (String one : value) {
            DataTypeEnum.DATE.formatCheck(one);
        }
    }
}
