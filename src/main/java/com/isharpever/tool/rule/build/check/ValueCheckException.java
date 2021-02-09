package com.isharpever.tool.rule.build.check;

public class ValueCheckException extends RuntimeException {
    private static final long serialVersionUID = -1663916418382378895L;

    public ValueCheckException(String message) {
        super(message);
    }

    public ValueCheckException(String message, Throwable cause) {
        super(message, cause);
    }
}
