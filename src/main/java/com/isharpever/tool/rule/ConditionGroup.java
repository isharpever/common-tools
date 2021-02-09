package com.isharpever.tool.rule;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Getter @Setter
public class ConditionGroup {
    private boolean not;
    private String cojunction;
    private List<Condition> conditions;
    private List<ConditionGroup> conditionGroups;
}
