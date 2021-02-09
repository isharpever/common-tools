package com.isharpever.tool.rule.build;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Getter
public class StatementBuildResult {
    private StringBuilder condition;
    private Set<String> imports;
    private Set<String> functions;
    private Set<String> factors;

    public StatementBuildResult() {
        this.condition = new StringBuilder();
        this.imports = new HashSet<>();
        this.functions = new HashSet<>();
        this.factors = new HashSet<>();
    }

    public void mergeFrom(StatementBuildResult from) {
        this.getCondition().append(from.getCondition());
        if (CollectionUtils.isNotEmpty(from.getFunctions())) {
            this.getFunctions().addAll(from.getFunctions());
        }
        if (CollectionUtils.isNotEmpty(from.getImports())) {
            this.getImports().addAll(from.getImports());
        }
        if (CollectionUtils.isNotEmpty(from.getFactors())) {
            this.getFactors().addAll(from.getFactors());
        }
    }

    public void addCondition(String condition) {
        this.condition.append(condition);
    }

    public void addCondition(StringBuilder condition) {
        this.condition.append(condition);
    }

    public void addFunctions(String... functions) {
        this.functions.addAll(Arrays.asList(functions));
    }

    public void addImports(String... imports) {
        this.imports.addAll(Arrays.asList(imports));
    }

    public void addFactors(String... factors) {
        this.factors.addAll(Arrays.asList(factors));
    }
}
