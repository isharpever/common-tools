package com.isharpever.tool.rule;

import com.isharpever.tool.rule.build.check.ValueCheckException;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public enum DataTypeEnum {
    NUMBER("number") {
        @Override
        public void formatCheck(String data) {
            if (!Pattern.matches("^[-]?\\d+$", data)) {
                log.error("data格式不是数字 data={}", data);
                throw new ValueCheckException("data格式不是数字");
            }
        }
    },
    BOOLEAN("boolean") {
        @Override
        public void formatCheck(String data) {
            if (!"true".equalsIgnoreCase(data) && !"false".equalsIgnoreCase(data)) {
                log.error("data无法转换成布尔值 data={}", data);
                throw new ValueCheckException("data无法转换成布尔值");
            }
        }
    },
    TEXT("text") {
        @Override
        public void formatCheck(String data) {
            if (data == null) {
                log.error("data不能是null");
                throw new ValueCheckException("data不能是null");
            }
        }
    },
    DATE("date") {
        @Override
        public void formatCheck(String data) {
            if (!Pattern.matches("^\\d{4}-\\d{2}-\\d{2}$", data)) {
                log.error("data格式不是yyyy-MM-dd data={}", data);
                throw new ValueCheckException("data格式不是yyyy-MM-dd");
            }
        }
    },
    DATETIME("datetime") {
        @Override
        public void formatCheck(String data) {
            if (!Pattern.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$", data)) {
                log.error("data格式不是yyyy-MM-dd HH:mm:ss data={}", data);
                throw new ValueCheckException("data格式不是yyyy-MM-dd HH:mm:ss");
            }
        }
    },
    TIME("time") {
        @Override
        public void formatCheck(String data) {
            if (!Pattern.matches("^\\d{2}:\\d{2}:\\d{2}$", data)) {
                log.error("data格式不是HH:mm:ss data={}", data);
                throw new ValueCheckException("data格式不是HH:mm:ss");
            }
        }
    },
    FACTOR("factor") {
        @Override
        public void formatCheck(String data) {
            if (data == null) {
                log.error("data不能是null");
                throw new ValueCheckException("data不能是null");
            }
        }
    },
    NULL("null") {
        @Override
        public void formatCheck(String data) {
            // do nothing
        }
    },
    OTHER("other") {
        @Override
        public void formatCheck(String data) {
            // do nothing
        }
    }
    ;

    private final String dataType;

    DataTypeEnum(String dataType) {
        this.dataType = dataType;
    }

    public String getDataType() {
        return this.dataType;
    }

    public abstract void formatCheck(String data);

    public static DataTypeEnum from(String dataType) {
        for (DataTypeEnum data : DataTypeEnum.values()) {
            if (data.dataType.equals(dataType)) {
                return data;
            }
        }
        return DataTypeEnum.OTHER;
    }
}
