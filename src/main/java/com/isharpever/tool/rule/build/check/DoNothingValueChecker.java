package com.isharpever.tool.rule.build.check;

import java.util.List;

public class DoNothingValueChecker implements ValueChecker {
    public static final DoNothingValueChecker INSTANCE = new DoNothingValueChecker();

    private DoNothingValueChecker() {}

    @Override
    public void check(List<String> value) throws ValueCheckException {
        // do nothing
    }
}
