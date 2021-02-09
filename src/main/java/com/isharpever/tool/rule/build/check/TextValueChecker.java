package com.isharpever.tool.rule.build.check;

import com.isharpever.tool.rule.DataTypeEnum;

import java.util.List;

public class TextValueChecker implements ValueChecker {
    public static final TextValueChecker INSTANCE = new TextValueChecker();

    private TextValueChecker() {}

    @Override
    public void check(List<String> value) throws ValueCheckException {
        NotEmptyValueChecker.INSTANCE.check(value);
        for (String one : value) {
            DataTypeEnum.TEXT.formatCheck(one);
        }
    }
}
