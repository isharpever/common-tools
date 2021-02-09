package com.isharpever.tool.rule.build;

import java.util.List;

public interface StatementBuilder {
    static final String IDENTITY_FORMAT = "%s_%s";
    static final String PARAMS_VARIABLE = "params";


    String identity();
    void register();
    StatementBuildResult build(String field, List<String> value);
}
