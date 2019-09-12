package com.isharpever.tool.dynamic;

import com.isharpever.tool.dynamic.compile.CompileUtil;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 动态bean的注册、卸载、执行、查看
 */
@RestController
@RequestMapping("/dynamic")
public class DynamicController implements BeanClassLoaderAware {

    private static final Logger log = LoggerFactory.getLogger(DynamicController.class);

    public static final String BEAN_NAME = "DynamicController";

    private ClassLoader beanClassLoader;

    @Resource
    private DynamicBeanRegistry dynamicBeanRegistry;

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

    /**
     * 注册动态bean
     * <br>自动更新引用了此脚本的其他脚本(重新加载类,重新注册动态bean)
     *
     * @param code 类脚本
     */
    @RequestMapping("/register")
    public String register(@RequestBody String code) {
        // 为此脚本注册一个动态bean
        DynamicBeanInfo dynamicBeanInfo = dynamicBeanRegistry.registerBean(code);
        if (dynamicBeanInfo == null) {
            return "fail";
        }

        // 若有其他脚本引用了此脚本,对应更新其他脚本的动态bean
        refreshDynamicBean(dynamicBeanInfo.getOriginalFullClassName());

        if (dynamicBeanInfo.registered()) {
            return "加载并注册了动态bean: " + dynamicBeanInfo.getOriginalBeanName();
        } else {
            return "只加载class,不创建动态bean";
        }
    }

    /**
     * 卸载一个动态bean
     *
     * @param beanName 指定bean name
     */
    @RequestMapping("/unregister/{beanName}")
    public String unregister(@PathVariable String beanName) {
        if (dynamicBeanRegistry.getDyanmicBean(beanName) == null) {
            return "查无此bean";
        }
        dynamicBeanRegistry.unregisterBean(beanName);
        if (dynamicBeanRegistry.getDyanmicBean(beanName) == null) {
            return "success";
        } else {
            return "fail";
        }
    }

    /**
     * 展示动态bean的class/源码信息
     *
     * @param beanName 指定bean name
     */
    @RequestMapping("/info/{beanName}")
    public String info(@PathVariable String beanName) {
        DynamicBeanInfo dynamicBeanInfo = dynamicBeanRegistry.getDyanmicBean(beanName);
        if (dynamicBeanInfo == null) {
            return "非动态bean";
        }
        String fullClassName = dynamicBeanInfo.getFullClassName();
        String source = CompileUtil.showSource(fullClassName);
        return new StringBuilder("class: ").append(fullClassName).append("\n\n")
                .append(source).toString();
    }

    /**
     * 展示包含指定路径的全部路径
     *
     * @param name
     * @return
     * @throws IOException
     */
    @RequestMapping("/debug/classpath/resource")
    public String resource(String name) throws IOException {
        StringBuilder result = new StringBuilder();
        result.append("包含资源[").append(name).append("]的路径:\n");
        Enumeration<URL> enumeration = this.beanClassLoader.getResources(name);
        while (enumeration.hasMoreElements()) {
            URL url = enumeration.nextElement();
            result.append(url).append("\n");
        }
        return result.toString();
    }

    /**
     * 展示全部动态class
     *
     * @return
     */
    @RequestMapping("/debug/dynamic/classes")
    public String classes() {
        StringBuilder result = new StringBuilder();
        result.append("全部动态class:\n");

        Map<String, DynamicBeanInfo> beans = this.dynamicBeanRegistry.allDynamicBeans();
        Map<String, String> classBeanMap = beans.values().stream().collect(Collectors
                .toMap(DynamicBeanInfo::getFullClassName, DynamicBeanInfo::getOriginalBeanName));

        List<String> classNames = CompileUtil.allQualifiedNames();
        for (String className : classNames) {
            result.append(className);
            String beanName = classBeanMap.get(className);
            if (beanName != null) {
                result.append(" - ").append(beanName);
            }
            result.append("\n");
        }
        result.append("数量:").append(classNames.size());
        return result.toString();
    }

    /**
     * 展示指定class的变更版本
     *
     * @param fullClassName 类全限定名
     * @return
     */
    @RequestMapping("/debug/class/version")
    public String classVersion(String fullClassName) {
        if (StringUtils.isBlank(fullClassName)) {
            return "未指定fullClassName(类全限定名)";
        }
        List<String> version = CompileUtil.version(fullClassName);
        if (version == null) {
            return "查无此类";
        }
        StringBuilder result = new StringBuilder();
        version.forEach(name -> result.append(name).append("\n"));
        return result.toString();
    }

    /**
     * 展示指定class的源码
     *
     * @param fullClassName 类全限定名
     * @return
     */
    @RequestMapping("/debug/class/source")
    public String classSource(String fullClassName) {
        if (StringUtils.isBlank(fullClassName)) {
            return "未指定fullClassName(类全限定名)";
        }

        // 初始化编译工具类
        CompileUtil.init(this.beanClassLoader);

        return CompileUtil.showSource(fullClassName);
    }

    private void refreshDynamicBean(String fullClassName) {
        // 引用此动态类的其他动态类(原始类名)
        Set<String> quoters = CompileUtil.findReferenceClasses(fullClassName);
        if (CollectionUtils.isEmpty(quoters)) {
            return;
        }

        quoters.forEach(quoter -> {
            // 刷新此类代码
            String finalClassName = CompileUtil.refreshSource(quoter);

            // 注册动态bean
            this.dynamicBeanRegistry.registerBean(quoter, finalClassName);
        });
    }
}
