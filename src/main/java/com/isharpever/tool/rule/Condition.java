package com.isharpever.tool.rule;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Getter @Setter
public class Condition {
    private String field;
    private String operator;
    private List<String> value;
    private String valueType;
}
