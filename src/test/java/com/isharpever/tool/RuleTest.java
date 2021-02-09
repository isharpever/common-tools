package com.isharpever.tool;

import com.isharpever.tool.rule.Rule;
import com.isharpever.tool.rule.RuleParser;
import com.isharpever.tool.rule.build.*;
import com.isharpever.tool.utils.GroovyCachedEngine;
import org.junit.Test;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuleTest {
    @Test
    public void testRule() {
        Equals_Number_StatementBuilder ensb = new Equals_Number_StatementBuilder();
        ensb.register();
        Equals_Boolean_StatementBuilder ebsb = new Equals_Boolean_StatementBuilder();
        ebsb.register();
        Equals_Text_StatementBuilder etsb = new Equals_Text_StatementBuilder();
        etsb.register();
        Equals_Date_StatementBuilder edsb = new Equals_Date_StatementBuilder();
        edsb.register();
        Equals_DateTime_StatementBuilder edtsb = new Equals_DateTime_StatementBuilder();
        edtsb.register();
        Equals_Time_StatementBuilder etmsb = new Equals_Time_StatementBuilder();
        etmsb.register();
        Equals_Factor_StatementBuilder efsb = new Equals_Factor_StatementBuilder();
        efsb.register();
        Contains_Number_StatementBuilder cnsb = new Contains_Number_StatementBuilder();
        cnsb.register();

        // 解析规则
        Rule rule = RuleParser.parse("{'not':false,'cojunction':'&&','conditions':[{'field':'age','operator':'==','value':['12'],'valueType':'number'},{'field':'fat','operator':'==','value':['false'],'valueType':'boolean'},{'field':'live','operator':'==','value':['HZ'],'valueType':'text'},{'field':'sd','operator':'==','value':['2011-02-01'],'valueType':'date'},{'field':'sdt','operator':'==','value':['2011-02-01 15:09:00'],'valueType':'datetime'},{'field':'st','operator':'==','value':['15:22:00'],'valueType':'time'}],'conditionGroups':[{'not':false,'cojunction':'&&','conditions':[{'field':'sala','operator':'==','value':['xf'],'valueType':'factor'},{'field':'nl','operator':'包含','value':[10,20],'valueType':'number'}]}]}");
        System.out.println(rule.getSourceCode());

        // 执行因子
        Map<String, Object> factorParams = new HashMap<>();
        Map<String, Object> factorResult = executeFactor(rule.getFactors(), factorParams);

        // 执行规则
        Object result = GroovyCachedEngine.evaluate(factorResult, rule.getSourceCode(), "rule.test");
        System.out.println(result);
    }

    private Map<String, Object> executeFactor(List<String> factors, Map<String, Object> factorParams) {
        // 查询因子脚本
        Map<String, String> scripts = queryFactorScript(factors);

        Map<String, Object> factorResult = new HashMap<>();
        for (String factor : factors) {
            // 执行因子脚本
            Object result = GroovyCachedEngine.evaluate(factorParams, scripts.get(factor), "factor." + factor);
            factorResult.put(factor, result);
        }
        return factorResult;
    }

    private Map<String, String> queryFactorScript(List<String> factors) {
        // 因子脚本库
        Map<String, String> factorScripts = new HashMap<>();
        factorScripts.put("age", "return 12;");
        factorScripts.put("height", "return 160;");
        factorScripts.put("fat", "return false;");
        factorScripts.put("live", "return \"HZ\";");
        factorScripts.put("sd", "import java.text.SimpleDateFormat;return new SimpleDateFormat(\"yyyy-MM-dd\").parse(\"2011-02-01\");");
        factorScripts.put("sdt", "import java.text.SimpleDateFormat;return new SimpleDateFormat(\"yyyy-MM-dd HH:mm:ss\").parse(\"2011-02-01 15:09:00\");");
        factorScripts.put("st", "import java.text.SimpleDateFormat;return new SimpleDateFormat(\"HH:mm:ss\").parse(\"15:22:00\");");
        factorScripts.put("sala", "return 75;");
        factorScripts.put("xf", "return 75;");
        factorScripts.put("nl", "return Arrays.asList(10, 20);");

        // 返回指定因子的脚本
        Map<String, String> result = new HashMap<>();
        for (String factor : factors) {
            result.put(factor, factorScripts.get(factor));
        }
        return result;
    }
}
