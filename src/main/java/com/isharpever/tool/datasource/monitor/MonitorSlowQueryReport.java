package com.isharpever.tool.datasource.monitor;

import com.dianwoba.monitor.client.MonitorFactory;
import com.dianwoba.monitor.client.MonitorPoint;
import com.dianwoba.monitor.client.MonitorUtil;
import com.isharpever.tool.enums.CustomLogLevel;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.jdbc.pool.PoolProperties.InterceptorProperty;
import org.apache.tomcat.jdbc.pool.interceptor.SlowQueryReport;

/**
 * 慢查询监控
 *
 * @author yinxiaolin
 * @date 2018/11/18
 */
public class MonitorSlowQueryReport extends SlowQueryReport {

    private static final Logger logger = LogManager.getLogger(MonitorSlowQueryReport.class);

    private String appCode;
    private String name = "";
    protected static final Set<String> SET_METHODS = new HashSet<String>();
    protected static final String SET_NULL  = "setNull";
    private MonitorUtil monitor = MonitorFactory.connect();

    static {
        SET_METHODS.add("setString");
        SET_METHODS.add("setNString");
        SET_METHODS.add("setInt");
        SET_METHODS.add("setByte");
        SET_METHODS.add("setShort");
        SET_METHODS.add("setLong");
        SET_METHODS.add("setDouble");
        SET_METHODS.add("setFloat");
        SET_METHODS.add("setTimestamp");
        SET_METHODS.add("setDate");
        SET_METHODS.add("setTime");
        SET_METHODS.add("setArray");
        SET_METHODS.add("setBigDecimal");
        SET_METHODS.add("setAsciiStream");
        SET_METHODS.add("setBinaryStream");
        SET_METHODS.add("setBlob");
        SET_METHODS.add("setBoolean");
        SET_METHODS.add("setBytes");
        SET_METHODS.add("setCharacterStream");
        SET_METHODS.add("setNCharacterStream");
        SET_METHODS.add("setClob");
        SET_METHODS.add("setNClob");
        SET_METHODS.add("setObject");
        SET_METHODS.add("setNull");
    }

    @Override
    public void setProperties(Map<String, InterceptorProperty> properties) {
        super.setProperties(properties);
        InterceptorProperty systemCodeProperty = properties.get("systemCode");
        if (systemCodeProperty != null) {
            this.setAppCode(systemCodeProperty.getValue());
        }
        InterceptorProperty appCodeProperty = properties.get("appCode");
        if (appCodeProperty != null) {
            this.setAppCode(appCodeProperty.getValue());
        }
        InterceptorProperty nameProperty = properties.get("name");
        if (nameProperty != null) {
            this.setName(nameProperty.getValue());
        }
    }

    @Override
    protected String reportSlowQuery(String query, Object[] args, String name, long start,
            long delta) {
        String sql = query == null && args != null && args.length > 0 ? (String) args[0] : query;
        if (sql == null && compare(EXECUTE_BATCH, name)) {
            sql = "batch";
        }

        if (this.maxQueries > 0 ) {
            QueryStats qs = this.getQueryStats(sql);
            if (qs != null) {
                qs.add(delta, start);
            }
        }

        if (this.isLogSlow() && sql != null) {
            String beautifulSql = sql.replaceAll("\\s+", " ");
            String md5 = DigestUtils.md5Hex(beautifulSql);
            logger.log(CustomLogLevel.DING.toLevel(), "【{}慢查】md5={}; 耗时={}; SQL={}; 参数={};",
                    this.getName(), md5, delta, beautifulSql, this.getSqlParameterString());

            MonitorPoint point = MonitorPoint
                    .monitorKey("isharpever.datasource.slowQuery")
                    .addTag("app", this.getAppCode())
                    .addTag("ip", this.getLocalHostAddress())
                    .addTag("name", this.getName())
                    .addField("cost", delta)
                    .addTag("sql", md5).build();
            monitor.writePoint(point);
        }

        return sql;
    }

    private String getLocalHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException var2) {
            logger.error("获取本地ip异常", var2);
            return "";
        }
    }

    @Override
    public Object createStatement(Object proxy, Method method, Object[] args, Object statement,
            long time) {
        try {
            Object result = null;
            String name = method.getName();
            String sql = null;
            Constructor<?> constructor = null;
            if (compare(CREATE_STATEMENT, name)) {
                //createStatement
                constructor = getConstructor(CREATE_STATEMENT_IDX, Statement.class);
            } else if (compare(PREPARE_STATEMENT, name)) {
                //prepareStatement
                sql = (String) args[0];
                constructor = getConstructor(PREPARE_STATEMENT_IDX, PreparedStatement.class);
                if (sql != null) {
                    prepareStatement(sql, time);
                }
            } else if (compare(PREPARE_CALL, name)) {
                //prepareCall
                sql = (String) args[0];
                constructor = getConstructor(PREPARE_CALL_IDX, CallableStatement.class);
                prepareCall(sql, time);
            } else {
                //do nothing, might be a future unsupported method
                //so we better bail out and let the system continue
                return statement;
            }
            result = constructor.newInstance(new RecordParamStatementProxy(statement, sql));
            return result;
        } catch (Exception x) {
            logger.warn("Unable to create statement proxy for slow query report.", x);
        }
        return statement;
    }

    protected String getSqlParameterString() {
        List<Object> typeList = new ArrayList<Object>(ParamHolder.params.get().size());
        for (Object value : ParamHolder.params.get()) {
            if (value == null) {
                typeList.add("null");
            } else {
                typeList.add(
                        objectValueString(value) + "(" + value.getClass().getSimpleName() + ")");
            }
        }
        return typeList.toString();
    }

    protected String objectValueString(Object value) {
        if (value instanceof Array) {
            return arrayValueString((Array) value);
        }
        return value.toString();
    }

    protected String arrayValueString(Array array) {
        try {
            Object value = array.getArray();
            if (value instanceof Object[]) {
                return Arrays.toString((Object[]) value);
            } else if (value instanceof long[]) {
                return Arrays.toString((long[]) value);
            } else if (value instanceof int[]) {
                return Arrays.toString((int[]) value);
            } else if (value instanceof short[]) {
                return Arrays.toString((short[]) value);
            } else if (value instanceof char[]) {
                return Arrays.toString((char[]) value);
            } else if (value instanceof byte[]) {
                return Arrays.toString((byte[]) value);
            } else if (value instanceof boolean[]) {
                return Arrays.toString((boolean[]) value);
            } else if (value instanceof float[]) {
                return Arrays.toString((float[]) value);
            } else {
                return Arrays.toString((double[]) value);
            }
        } catch (SQLException e) {
            return array.toString();
        }
    }

    protected String getAppCode() {
        return appCode;
    }

    protected void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    protected String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    class RecordParamStatementProxy extends StatementProxy {

        RecordParamStatementProxy(Object parent, String query) {
            super(parent, query);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (SET_METHODS.contains(method.getName())) {
                if (SET_NULL.equals(method.getName())) {
                    ParamHolder.params.get().add(null);
                }else if (args != null && args.length >= 2) {
                    ParamHolder.params.get().add(args[1]);
                }
            }

            Object result = null;
            try {
                result = super.invoke(proxy, method, args);
            } finally {
                if (MonitorSlowQueryReport.this.isExecute(method, false)) {
                    ParamHolder.params.remove();
                }
            }

            return result;
        }
    }
}
