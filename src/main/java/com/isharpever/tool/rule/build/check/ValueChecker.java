package com.isharpever.tool.rule.build.check;

import java.util.List;

public interface ValueChecker {
    void check(List<String> value) throws ValueCheckException;
}
