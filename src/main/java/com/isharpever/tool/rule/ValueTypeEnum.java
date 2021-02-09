package com.isharpever.tool.rule;

import com.isharpever.tool.rule.build.check.ValueCheckException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

@Slf4j
public enum ValueTypeEnum {
    NUMBER("number") {
        @Override
        public void formatCheck(String value) {
            if (!Pattern.matches("^[-]?\\d+$", value)) {
                log.error("value格式不是数字 value={}", value);
                throw new ValueCheckException("value格式不是数字");
            }
        }
    },
    BOOLEAN("boolean") {
        @Override
        public void formatCheck(String value) {
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                log.error("value无法转换成布尔值 value={}", value);
                throw new ValueCheckException("value无法转换成布尔值");
            }
        }
    },
    TEXT("text") {
        @Override
        public void formatCheck(String value) {
            // do nothing
        }
    },
    DATE("date") {
        @Override
        public void formatCheck(String value) {
            if (!Pattern.matches("^\\d{4}-\\d{2}-\\d{2}$", value)) {
                log.error("value格式不是yyyy-MM-dd value={}", value);
                throw new ValueCheckException("value格式不是yyyy-MM-dd");
            }
        }
    },
    DATETIME("datetime") {
        @Override
        public void formatCheck(String value) {
            if (!Pattern.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$", value)) {
                log.error("value格式不是yyyy-MM-dd HH:mm:ss value={}", value);
                throw new ValueCheckException("value格式不是yyyy-MM-dd HH:mm:ss");
            }
        }
    },
    TIME("time") {
        @Override
        public void formatCheck(String value) {
            if (!Pattern.matches("^\\d{2}:\\d{2}:\\d{2}$", value)) {
                log.error("value格式不是HH:mm:ss value={}", value);
                throw new ValueCheckException("value格式不是HH:mm:ss");
            }
        }
    },
    FACTOR("factor") {
        @Override
        public void formatCheck(String value) {
            // do nothing
        }
    },
    OTHER("other") {
        @Override
        public void formatCheck(String value) {
            // do nothing
        }
    }
    ;

    private final String valueType;

    ValueTypeEnum(String valueType) {
        this.valueType = valueType;
    }

    public String getValueType() {
        return this.valueType;
    }

    public abstract void formatCheck(String value);

    public static ValueTypeEnum from(String valueType) {
        for (ValueTypeEnum value : ValueTypeEnum.values()) {
            if (value.valueType.equals(valueType)) {
                return value;
            }
        }
        return ValueTypeEnum.OTHER;
    }
}
