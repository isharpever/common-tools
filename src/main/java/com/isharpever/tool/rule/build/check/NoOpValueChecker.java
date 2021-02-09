package com.isharpever.tool.rule.build.check;

import java.util.List;

public class NoOpValueChecker implements ValueChecker {
    public static final NoOpValueChecker INSTANCE = new NoOpValueChecker();

    private NoOpValueChecker() {}

    @Override
    public void check(List<String> value) throws ValueCheckException {
        // do nothing
    }
}
