package com.isharpever.tool.rule;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Getter @Setter
public class Rule {
    private String sourceCode;
    private List<String> factors;
}
