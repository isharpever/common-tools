package com.isharpever.tool.dynamic.compile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 代码(字符串)编译加载成Class
 */
public class CompileUtil {

    private static final Logger log = LoggerFactory.getLogger(CompileUtil.class);

    /** 兼容springboot fat-jar */
    private static final String SPRING_BOOT_CLASS_PATH_PREFIX = "BOOT-INF/classes/";

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w.]+)\\s*;");

    private static final Pattern CLASS_PATTERN = Pattern.compile("class\\s+(\\w+)\\s+");

    private static final Pattern IMPORT_PATTERN = Pattern.compile("(import|import static)\\s+[\\w.]+\\s*;");

    private static final Pattern IMPORT_ANNO_PATTERN = Pattern.compile("@ImportClass\\(\"([\\w.]+)\"\\)");

    private static final ClassSourceManager classSourceManager = new ClassSourceManager();

    private static volatile ClassLoader basicClassLoader;

    private static volatile List<String> options;

    private static volatile Method defineClass;

    /**
     * 记录加载过的类
     * <li>key:full class name
     * <li>value:class
     */
    private static final Map<String, Class<?>> LOAD_CLASS_CACHE = new HashMap<>();

    /**
     * 初始化编译工具类
     * @param classLoader
     */
    public static void init(ClassLoader classLoader) {
        if (options != null) {
            return;
        }
        synchronized (CompileUtil.class) {
            if (options != null) {
                return;
            }

            options = new ArrayList<>();
            options.add("-source");
            options.add("1.8");
            options.add("-target");
            options.add("1.8");

            basicClassLoader = classLoader;

            try {
                defineClass = ClassLoader.class
                        .getDeclaredMethod("defineClass", String.class, byte[].class, int.class,
                                int.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(ExceptionUtils.getStackTrace(e));
            }
        }
    }

    /**
     * 添加源码
     *
     * @param code
     * @return 类全限定名
     */
    public static String addSource(String code) {
        code = code.replaceAll("\r\n", "\n");
        code = code.replace("\\r\\n", "\n");

        String currQualifiedName = getQualifiedName(code);
        String originalQualifiedName = findOriginalQualifiedName(currQualifiedName);

        synchronized (originalQualifiedName.intern()) {
            code = classSourceManager.preProcess(code);

            String newQualifiedName = getQualifiedName(code);
            classSourceManager.addSource(newQualifiedName, code);

            return newQualifiedName;
        }
    }

    /**
     * 刷新指定类代码
     * @param qualifiedName 类全限定名
     * @return
     */
    public static String refreshSource(String qualifiedName) {
        String originalQualifiedName = findOriginalQualifiedName(qualifiedName);

        synchronized (originalQualifiedName.intern()) {
            // 指定类最新代码
            String finalQualifiedName = findFinalQualifiedName(originalQualifiedName);
            String sourceCode = classSourceManager.getSourceCode(finalQualifiedName);
            if (StringUtils.isBlank(sourceCode)) {
                String error = String.format("取指定类的最新代码失败 指定类=%s 原始类=%s 最新类=%s",
                        qualifiedName, originalQualifiedName, finalQualifiedName);
                throw new IllegalStateException(error);
            }
            return addSource(sourceCode);
        }
    }

    /**
     * 显示指定类的源代码
     * @param qualifiedName 类全限定名
     * @return
     */
    public static String showSource(String qualifiedName) {
        return classSourceManager.getSourceCode(qualifiedName);
    }

    /**
     * 移除指定类
     * @param qualifiedName
     */
    public void removeClass(String qualifiedName) {
        classSourceManager.removeClass(qualifiedName);
    }

    /**
     * 返回指定类的版本
     * @param qualifiedName
     * @return
     */
    public static List<String> version(String qualifiedName) {
        return classSourceManager.version(qualifiedName);
    }

    /**
     * 返回指定类的原始类名
     * @param qualifiedName 类全限定名
     * @return
     */
    public static String findOriginalQualifiedName(String qualifiedName) {
        return classSourceManager.findOriginalQualifiedName(qualifiedName);
    }

    /**
     * 返回指定类的最新类名
     * @param qualifiedName 类全限定名
     * @return
     */
    public static String findFinalQualifiedName(String qualifiedName) {
        return classSourceManager.findFinalQualifiedName(qualifiedName);
    }

    /**
     * 返回引用了此类的其他类
     * @param qualifiedName 类的全限定名
     * @return
     */
    public static Set<String> findReferenceClasses(String qualifiedName) {
        return classSourceManager.findReferenceClasses(qualifiedName);
    }

    /**
     * 从源代码获取类全限定名
     * @param code
     * @return
     */
    public static String getQualifiedName(String code) {
        return classSourceManager.getQualifiedName(code);
    }

    /**
     * 返回全部动态class name,按名称排序
     * @return
     */
    public static List<String> allQualifiedNames() {
        List<String> result = classSourceManager.allQualifiedNames();
        Collections.sort(result);
        return result;
    }

    /**
     * 用指定classLoader加载指定类名(先读ClassLoader自身缓存)
     *
     * @param classLoader
     * @param qualifiedName
     * @return
     */
    public static Class<?> load(ClassLoader classLoader, String qualifiedName) {
        Class<?> clazz = LOAD_CLASS_CACHE.get(qualifiedName);
        if (clazz == null) {
            synchronized (LOAD_CLASS_CACHE) {
                clazz = LOAD_CLASS_CACHE.get(qualifiedName);
                if (clazz == null) {
                    clazz = define(classLoader, qualifiedName);
                    LOAD_CLASS_CACHE.put(qualifiedName, clazz);
                }
            }
        }
        return clazz;
    }

    /**
     * 用指定classLoader加载指定类名(先读ClassLoader自身缓存)
     *
     * @param classLoader
     * @param qualifiedName
     * @return
     */
    private static Class<?> define(ClassLoader classLoader, String qualifiedName) {
        // 先编译
        JavaClassFileObject classFileObject = doCompile(qualifiedName);
        if (classFileObject == null) {
            throw new IllegalStateException(
                    String.format("Loading failed, unknown error. class: %s", qualifiedName));
        }

        byte[] byteCodes = classFileObject.getByteCode();
        defineClass.setAccessible(true);
        try {
            // 如果前面不走ClassLoader自身缓存,直接调用这个方法,重复load时会抛出异常
            // java.lang.LinkageError: loader (instance of XXX): attempted duplicate class definition for name: XXX
            // 如果需要重复load，有两种途径：实例化一个新的classloader、变更类名
            return (Class<?>) defineClass
                    .invoke(classLoader, qualifiedName, byteCodes, 0, byteCodes.length);
        } catch(Exception e) {
            throw new IllegalStateException(ExceptionUtils.getStackTrace(e));
        } finally {
            defineClass.setAccessible(false);
        }
    }

    /**
     * 指定类名是否是已加载的动态类
     * @param qualifiedName
     * @return
     */
    public static boolean isLoadedDynamicClass(String qualifiedName) {
        return LOAD_CLASS_CACHE.containsKey(qualifiedName);
    }

    /**
     * 编译
     *
     * @param qualifiedName
     * @return
     */
    private static JavaClassFileObject doCompile(String qualifiedName) {
        JavaSourceFileObject sourceFileObjects = classSourceManager.getSourceFileObject(qualifiedName);
        if (sourceFileObjects == null) {
            throw new IllegalStateException(
                    String.format("Compilation failed, source code not found. class: %s",
                            qualifiedName));
        }

        Set<String> importClassAndPackagePath = fetchImportClassAndPackagePath(sourceFileObjects.getSource());

        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        JavaFileManagerImpl javaFileManager = new JavaFileManagerImpl(
                compiler.getStandardFileManager(null, null, null),
                new ClasspathResolver(basicClassLoader, importClassAndPackagePath),
                classSourceManager);

        Boolean result;
        try {
            result = compiler.getTask(null, javaFileManager, diagnosticCollector, options,
                    null, Collections.singletonList(sourceFileObjects)).call();
        } finally {
            try {
                javaFileManager.close();
            } catch (IOException e) {
            }
        }
        if (result == null || !result) {
            throw new IllegalStateException(
                    "Compilation failed. class: " + sourceFileObjects + ", diagnostics: " + toString(
                            diagnosticCollector));
        }

        return classSourceManager.getClassFileObject(qualifiedName);
    }

    private static Set<String> fetchImportClassAndPackagePath(String source) {
        if (StringUtils.isBlank(source)) {
            return null;
        }

        Set<String> result = new HashSet<>();

        String packageName = classSourceManager.getPackageName(source);
        if (StringUtils.isNotBlank(packageName)) {
            result.add(packageName.replace(".", "/"));
        }

        Set<String> imports = classSourceManager.getImportClasses(source);
        if (CollectionUtils.isNotEmpty(imports)) {
            result.addAll(imports.stream()
                    .filter(className -> !className.startsWith("java") && !className.startsWith("com.sun"))
                    .map(className -> className.replace(".", "/") + ".class")
                    .collect(Collectors.toSet()));
        }

        // 兼容springboot fat-jar
        result.addAll(result.stream().map(path -> SPRING_BOOT_CLASS_PATH_PREFIX + path)
                .collect(Collectors.toSet()));

        return result;
    }

    private static String toString(DiagnosticCollector<JavaFileObject> diagnosticCollector) {
        StringBuilder sb = new StringBuilder();
        diagnosticCollector.getDiagnostics().forEach(item -> {
            sb.append(item.toString());
        });
        return sb.toString();
    }

    private static final class ClassSourceManager {
        /** 编译前源码 */
        private final Map<String, JavaSourceFileObject> inputFileObjects = new HashMap<>();

        /** 编译后字节码 */
        private final Map<String, JavaClassFileObject> outputFileObjects = new HashMap<>();

        /**
         * 记录全部原始类名
         */
        private final Set<String> ALL_ORIGINAL_CLASS_NAME = new HashSet<>();

        /**
         * 记录一个类的原始类名
         * <li>key:变更后full class name
         * <li>value:该类原始的full class name
         */
        private final Map<String, String> ORIGINAL_CLASS_NAME_MAP = new ConcurrentHashMap<>();

        /**
         * 记录类的被引用关系
         * <li>key:被引用类的full class name
         * <li>value:引用该类的其他类的full class name
         */
        private final Map<String, Set<String>> CLASS_REFERENCE_MAP = new ConcurrentHashMap<>();

        /**
         * 记录类代码版本
         * <li>key:类原始的full class name
         * <li>value:改类历次变更的类名
         */
        private final Map<String, LinkedList<String>> CLASS_VERSION_MAP = new ConcurrentHashMap<>();

        Map<String, JavaSourceFileObject> getInputFileObjects() {
            return inputFileObjects;
        }

        Map<String, JavaClassFileObject> getOutputFileObjects() {
            return outputFileObjects;
        }

        /**
         * 新增类源码
         * @param qualifiedName
         * @param code
         */
        void addSource(String qualifiedName, String code) {
            code = code.replaceAll("\r\n", "\n");
            code = code.replace("\\r\\n", "\n");

            JavaSourceFileObject sourceFileObject = new JavaSourceFileObject(qualifiedName, code);
            this.inputFileObjects.put(qualifiedName, sourceFileObject);
        }

        JavaSourceFileObject getSourceFileObject(String qualifiedNames) {
            return inputFileObjects.get(qualifiedNames);
        }

        String getSourceCode(String qualifiedNames) {
            JavaSourceFileObject sourceFileObject = getSourceFileObject(qualifiedNames);
            if (sourceFileObject != null) {
                return sourceFileObject.getSource();
            }
            return null;
        }

        void addClassFileObject(String qualifiedName, JavaClassFileObject classFileObject) {
            this.outputFileObjects.put(qualifiedName, classFileObject);
        }

        JavaClassFileObject getClassFileObject(String qualifiedName) {
            return this.outputFileObjects.get(qualifiedName);
        }

        /**
         * 预处理源代码
         * @param code
         * @return
         */
        private String preProcess(String code) {
            String currQualifiedName = getQualifiedName(code);
            String originalQualifiedName = findOriginalQualifiedName(currQualifiedName);

            // 记录原始类名
            ALL_ORIGINAL_CLASS_NAME.add(originalQualifiedName);

            // 更新类的引用关系
            // 一定要在replaceClassNameIfNessary之前做
            refreshClassReference(originalQualifiedName, code);

            // 预处理源代码(替换类名)
            return replaceClassNameIfNessary(code, currQualifiedName, originalQualifiedName);
        }

        /**
         * 预处理源代码
         * <li>变更自身类名
         * <li>替换其他变更过的类名
         *
         * @param code 源码
         * @return
         */
        private String replaceClassNameIfNessary(String code) {
            String currQualifiedName = getQualifiedName(code);
            String originalQualifiedName = findOriginalQualifiedName(currQualifiedName);
            return replaceClassNameIfNessary(code, currQualifiedName, originalQualifiedName);
        }

        /**
         * 预处理源代码
         * <li>变更自身类名
         * <li>替换其他变更过的类名
         *
         * @param code 源码
         * @param currQualifiedName 源码当前的类全限定名
         * @param originalQualifiedName 源码原始的类全限定名
         * @return
         */
        private String replaceClassNameIfNessary(String code, String currQualifiedName,
                String originalQualifiedName) {
            // 当前的类名
            String oldSimpleClassName = getSimpleClassName(currQualifiedName);

            // 原始的类名
            String originalSimpleClassName = getSimpleClassName(originalQualifiedName);

            // 类如果已加载,需要改类名
            if (isLoadedDynamicClass(currQualifiedName)) {
                // 变更新类名
                String newSimpleClassName = String.format("%s%s", originalSimpleClassName, System.currentTimeMillis());
                code = code.replaceAll(toWordReg(oldSimpleClassName), newSimpleClassName);

                // 如果源码未显式extends,则以原始类作为父类
                String oldExtendsStatement = String.format("class\\s+%s\\s+extends\\s+[.\\w]+", newSimpleClassName);
                String newExtendsStatement = String.format("class %s extends %s", newSimpleClassName, originalSimpleClassName);
                if (contains(code, oldExtendsStatement)) {
                    // 源码有显式extends
                } else {
                    code = code.replaceAll(String.format("class\\s+%s", newSimpleClassName), newExtendsStatement);
                }
            }

            // 如果引用了其他脚本类,同时把其他脚本类替换为对应的最新类名(只支持[以全限定名引用]的情况)
            // 先替换为原始类名
            for (Entry<String, String> entry : ORIGINAL_CLASS_NAME_MAP.entrySet()) {
                // 按原始类名排除自己
                if (entry.getValue().equals(originalQualifiedName)) {
                    continue;
                }

                if (contains(code, toWordReg(entry.getKey()))) {
                    code = code.replaceAll(toWordReg(entry.getKey()), entry.getValue());
                }
            }
            // 再替换为最新类名
            for (Entry<String, LinkedList<String>> entry : CLASS_VERSION_MAP.entrySet()) {
                // 按原始类名排除自己
                if (entry.getKey().equals(originalQualifiedName)) {
                    continue;
                }

                if (contains(code, toWordReg(entry.getKey()))) {
                    code = code.replaceAll(toWordReg(entry.getKey()), entry.getValue().getLast());
                }
            }

            // 记录类名变更
            String newQualifiedName = getQualifiedName(code);
            if (!newQualifiedName.equals(originalQualifiedName)) {
                ORIGINAL_CLASS_NAME_MAP.put(newQualifiedName, originalQualifiedName);
                CLASS_VERSION_MAP.get(originalQualifiedName).addLast(newQualifiedName);
            } else if (CLASS_VERSION_MAP.get(originalQualifiedName) == null) {
                LinkedList<String> versions = new LinkedList<>();
                versions.addLast(originalQualifiedName);
                CLASS_VERSION_MAP.putIfAbsent(originalQualifiedName, versions);
            }

            return code;
        }

        /**
         * 返回类名
         * @param qualifiedName 类全限定名
         * @return
         */
        private static String getSimpleClassName(String qualifiedName) {
            return qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1);
        }

        /**
         * 更新类的引用关系
         * @param quoter 引用者的类全限定名
         * @param quoterCode 引用者的源码
         */
        private void refreshClassReference(String quoter, String quoterCode) {
            // 首先从引用关系中移除全部[引用者=quoter]的
            CLASS_REFERENCE_MAP.forEach((key, value) -> value.remove(quoter));

            // 重新建立[引用者=quoter]的引用关系
            ALL_ORIGINAL_CLASS_NAME.forEach(referred -> {
                if (!referred.equals(quoter) && contains(quoterCode, toWordReg(referred))) {
                    addClassReference(referred, quoter);
                }
            });
        }

        /**
         * 记录类的被引用关系
         * @param referred 被引用者的类全限定名
         * @param quoter 引用者的类全限定名
         */
        private void addClassReference(String referred, String quoter) {
            Set<String> otherClasses = CLASS_REFERENCE_MAP.get(referred);
            if (otherClasses == null) {
                CLASS_REFERENCE_MAP.putIfAbsent(referred, new HashSet<>());
            }
            CLASS_REFERENCE_MAP.get(referred).add(quoter);
        }

        /**
         * 返回引用了此类的其他类
         * @param qualifiedName 类的全限定名
         * @return
         */
        private Set<String> findReferenceClasses(String qualifiedName) {
            if (CLASS_REFERENCE_MAP.get(qualifiedName) == null) {
                return null;
            }
            return new HashSet<>(CLASS_REFERENCE_MAP.get(qualifiedName));
        }

        /**
         * 返回指定类的原始类名
         * @param qualifiedName 类全限定名
         * @return
         */
        private String findOriginalQualifiedName(String qualifiedName) {
            return ORIGINAL_CLASS_NAME_MAP.getOrDefault(qualifiedName, qualifiedName);
        }

        /**
         * 返回指定类的最新类名
         * <br>非线程安全,使用者需要根据实际情况自己保证线程安全
         *
         * @param qualifiedName 类全限定名
         * @return
         */
        private String findFinalQualifiedName(String qualifiedName) {
            String originalQualifiedName = findOriginalQualifiedName(qualifiedName);
            LinkedList<String> versions = CLASS_VERSION_MAP.get(originalQualifiedName);
            if (versions == null) {
                return qualifiedName;
            }
            return versions.getLast();
        }

        /**
         * 移除指定类相关关系
         * @param qualifiedName
         */
        private void removeClass(String qualifiedName) {
            if (StringUtils.isBlank(qualifiedName)) {
                return;
            }

            inputFileObjects.remove(qualifiedName);
            outputFileObjects.remove(qualifiedName);

            // 此类的原始类名
            String originalQualifiedName = findOriginalQualifiedName(qualifiedName);
            if (originalQualifiedName == null) {
                return;
            }

            // 移除此类的原始类名记录
            ORIGINAL_CLASS_NAME_MAP.remove(qualifiedName);

            // 移除前:此类的最后一个版本
            String finalQualifiedName = CLASS_VERSION_MAP.get(originalQualifiedName).getLast();

            // 移除类版本
            CLASS_VERSION_MAP.get(originalQualifiedName).remove(qualifiedName);

            // 如果要移除的类是原始版本,移除[引用者=此类]和[被引用者=此类]的引用关系
            if (qualifiedName.equals(originalQualifiedName)) {
                CLASS_VERSION_MAP.remove(originalQualifiedName);
                CLASS_REFERENCE_MAP.forEach((key, value) -> value.remove(originalQualifiedName));
            }
            // 如果要移除的类是移除前的最后一个版本,重建[引用者=此类]的引用关系
            else if (qualifiedName.equals(finalQualifiedName)) {
                finalQualifiedName = CLASS_VERSION_MAP.get(originalQualifiedName).getLast();
                String sourceCode = getSourceCode(finalQualifiedName);
                if (StringUtils.isNotBlank(sourceCode)) {
                    refreshClassReference(originalQualifiedName, sourceCode);
                }
            }
        }

        /**
         * 返回指定类的历次变更版本
         * @param qualifiedName
         * @return
         */
        private List<String> version(String qualifiedName) {
            String originalQualifiedName = findOriginalQualifiedName(qualifiedName);
            return CLASS_VERSION_MAP.get(originalQualifiedName);
        }

        private List<String> allQualifiedNames() {
            return new ArrayList<>(inputFileObjects.keySet());
        }

        private static String toWordReg(String className) {
            return "\\b" + className + "\\b";
        }

        private static boolean contains(String input, String regex) {
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(input);
            return m.find();
        }

        /**
         * 从源代码获取import classes
         * @param code
         * @return
         */
        private Set<String> getImportClasses(String code) {
            code = code.replaceAll("\r\n", "\n");
            code = code.replace("\\r\\n", "\n");

            Set<String> imports = new HashSet<>();

            Matcher matcher = IMPORT_PATTERN.matcher(code);
            while (matcher.find()) {
                imports.add(matcher.group(0));
            }

            Set<String> result = imports.stream().map(className -> {
                if (className.startsWith("import static ")) {
                    int pos = className.lastIndexOf(".");
                    return className.substring("import static ".length(), pos).trim();
                } else {
                    return className.substring("import ".length(), className.length() - 1).trim();
                }
            }).collect(Collectors.toSet());

            // @ImportClass注解
            matcher = IMPORT_ANNO_PATTERN.matcher(code);
            while (matcher.find()) {
                result.add(matcher.group(1));
            }

            return result;
        }

        /**
         * 从源代码获取package
         * @param code
         * @return
         */
        private String getPackageName(String code) {
            code = code.replaceAll("\r\n", "\n");
            code = code.replace("\\r\\n", "\n");

            Matcher matcher = PACKAGE_PATTERN.matcher(code);
            String pkg;
            if (matcher.find()) {
                pkg = matcher.group(1);
            } else {
                pkg = "";
            }

            return pkg;
        }

        /**
         * 从源代码获取类全限定名
         * @param code
         * @return
         */
        private String getQualifiedName(String code) {
            code = code.replaceAll("\r\n", "\n");
            code = code.replace("\\r\\n", "\n");

            Matcher matcher = CLASS_PATTERN.matcher(code);
            String cls;
            if (matcher.find()) {
                cls = matcher.group(1);
            } else {
                throw new IllegalArgumentException("No class name in " + code);
            }

            String pkg = getPackageName(code);
            return pkg != null && pkg.length() > 0 ? pkg + "." + cls : cls;
        }
    }

    public static final class JavaFileManagerImpl extends ForwardingJavaFileManager<JavaFileManager> {

        private ClasspathResolver classpathResolver;
        private ClassSourceManager classSourceManager;

        JavaFileManagerImpl(JavaFileManager fileManager, ClasspathResolver classpathResolver, ClassSourceManager classSourceManager) {
            super(fileManager);
            this.classpathResolver = classpathResolver;
            this.classSourceManager = classSourceManager;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String qualifiedName,
                Kind kind, FileObject inputFile) {
            JavaClassFileObject outputFile = new JavaClassFileObject(qualifiedName, kind);
            this.classSourceManager.addClassFileObject(qualifiedName, outputFile);
            return outputFile;
        }

        @Override
        public String inferBinaryName(Location location, JavaFileObject file) {
            if (file instanceof NodeJavaFileObject) {
                return ((NodeJavaFileObject)file).binaryName;
            } else if (file instanceof JavaSourceFileObject) {
                return ((JavaSourceFileObject)file).binaryName;
            } else if (file instanceof JavaClassFileObject) {
                return ((JavaClassFileObject)file).binaryName;
            } else {
                return fileManager.inferBinaryName(location, file);
            }
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds,
                boolean recurse) throws IOException {
            if (location == StandardLocation.PLATFORM_CLASS_PATH) {
                return fileManager.list(location, packageName, kinds, recurse);
            } else if (location == StandardLocation.CLASS_PATH && kinds.contains(Kind.CLASS)) {
                if (packageName.startsWith("java") || packageName.startsWith("com.sun")) {
                    return fileManager.list(location, packageName, kinds, recurse);
                }

                List<JavaFileObject> result = new ArrayList<>();
                try {
                    result.addAll(classpathResolver.resolve(packageName, recurse));
                } catch (URISyntaxException e) {
                    throw new IllegalStateException(ExceptionUtils.getStackTrace(e));
                }

                this.classSourceManager.getOutputFileObjects().forEach((key, value) -> {
                    if (key.startsWith(packageName)) {
                        result.add(value);
                    }
                });

                return result;
            } else {
                return Collections.emptyList();
            }
        }
    }

    private static final class JavaSourceFileObject extends SimpleJavaFileObject {
        private String binaryName;

        /** input:源代码 */
        private final String source;

        JavaSourceFileObject(final String qualifiedName, final String source) {
            super(uri(qualifiedName), Kind.SOURCE);
            this.source = source;
            this.binaryName = qualifiedName;
        }

        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors) throws UnsupportedOperationException {
            if (source == null) {
                throw new UnsupportedOperationException("source == null");
            }
            return source;
        }

        String getSource() {
            return this.source;
        }

        private static URI uri(String qualifiedName) {
            try {
                return new URI(qualifiedName.replace(".", "/") + Kind.SOURCE.extension);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class JavaClassFileObject extends SimpleJavaFileObject {
        private String binaryName;

        /** output:字节码 */
        private ByteArrayOutputStream bytecode;

        JavaClassFileObject(final String qualifiedName, final Kind kind) {
            super(uri(qualifiedName), kind);
            this.binaryName = qualifiedName;
        }

        @Override
        public InputStream openInputStream() {
            return new ByteArrayInputStream(getByteCode());
        }

        @Override
        public OutputStream openOutputStream() {
            return bytecode = new ByteArrayOutputStream();
        }

        public byte[] getByteCode() {
            return bytecode.toByteArray();
        }

        private static URI uri(String qualifiedName) {
            try {
                return new URI(qualifiedName.replace(".", "/") + Kind.CLASS.extension);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
