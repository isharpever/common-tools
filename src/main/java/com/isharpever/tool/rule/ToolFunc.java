package com.isharpever.tool.rule;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;

@Slf4j
public class ToolFunc {
    public static int compareNumber(Number left, String right) {
        return new BigDecimal(String.valueOf(left)).compareTo(new BigDecimal(right));
    }
    
    public static boolean containsNumber(Collection<Object> left, String right) {
        for (Object number : left) {
            try {
                if (new BigDecimal(String.valueOf(number)).compareTo(new BigDecimal(right)) == 0) {
                    return true;
                }
            } catch (Exception e) {
                log.error("containsNumber异常 left={} right={}", left, right, e);
            }
        }
        return false;
    }
    
    public static int compareHms(Date left, String right) throws ParseException {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(left.toInstant(), ZoneId.systemDefault());
        LocalTime leftTime = localDateTime.toLocalTime().withNano(0);
        LocalTime rightTime = LocalTime.parse(right, DateTimeFormatter.ofPattern("HH:mm:ss"));
        return leftTime.compareTo(rightTime);
    }
    
    public static int compareYmdHms(Date left, String right) {
        LocalDateTime leftDateTime = LocalDateTime.ofInstant(left.toInstant(), ZoneId.systemDefault());
        LocalDateTime rightDateTime = LocalDateTime.parse(right, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return leftDateTime.compareTo(rightDateTime);
    }
    
    public static int compareYmd(Date left, String right) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(left.toInstant(), ZoneId.systemDefault());
        LocalDate leftDate = localDateTime.toLocalDate();
        LocalDate rightDate = LocalDate.parse(right, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return leftDate.compareTo(rightDate);
    }
}
