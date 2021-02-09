package com.isharpever.tool.rule.build.check;

import org.apache.commons.collections.CollectionUtils;

import java.util.List;

public class NotEmptyValueChecker implements ValueChecker {
    public static final NotEmptyValueChecker INSTANCE = new NotEmptyValueChecker();

    private NotEmptyValueChecker() {}

    @Override
    public void check(List<String> value) throws ValueCheckException {
        if (CollectionUtils.isEmpty(value)) {
            throw new ValueCheckException("value不能为空");
        }
    }
}
