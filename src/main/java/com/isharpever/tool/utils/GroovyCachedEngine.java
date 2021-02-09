package com.isharpever.tool.utils;

import com.isharpever.tool.rule.build.StatementBuilder;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilationFailedException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class GroovyCachedEngine {
    public static final Map<String, Script> scriptMap = new ConcurrentHashMap<>();
    public static final GroovyShell shell = new GroovyShell();

    public static Object evaluate(Map<String, Object> params, String scriptText, String scriptKey) throws CompilationFailedException {
        Script script = scriptMap.get(scriptKey);
        if (script == null) {
            synchronized (scriptMap) {
                script = scriptMap.get(scriptKey);
                if (script == null) {
                    GroovyShell shell = new GroovyShell();
                    script = shell.parse(scriptText);
                    scriptMap.put(scriptKey, script);
                }
            }
        }

        Binding binding = new Binding();
        binding.setVariable(StatementBuilder.PARAMS_VARIABLE, params);
        script.setBinding(binding);
        return script.run();
    }

    public static void refresh(String scriptText, String scriptKey) {
        GroovyShell shell = new GroovyShell();
        Script script = shell.parse(scriptText);
        scriptMap.put(scriptKey, script);
    }
}
