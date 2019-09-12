package com.isharpever.tool.dynamic.compile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.tools.JavaFileObject;
import org.crsh.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClasspathResolver {
    private static final Logger log = LoggerFactory.getLogger(ClasspathResolver.class);

    /** 兼容springboot fat-jar */
    private static final String SPRING_BOOT_CLASS_PREFIX = "BOOT-INF.classes.";

    /** . */
    final ClassLoader loader;

    /** . */
    final URLDriver driver;

    final Set<String> resourcePath;

    public ClasspathResolver(ClassLoader loader) {
        this(loader, Collections.singleton("META-INF/MANIFEST.MF"));
    }

    public ClasspathResolver(ClassLoader loader, Set<String> resourcePath) {

        URLDriver driver = null;
        try {
            driver = new URLDriver();
            driver.merge(loader, resourcePath);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //
        this.loader = loader;
        this.driver = driver;
        this.resourcePath = resourcePath;
    }

    private void resolve(List<JavaFileObject> files, Node node, String binaryName, boolean recurse) throws IOException, URISyntaxException {
        for (Node child : driver.children(node)) {
            Iterator<Resource> i = child.iterator();
            if (i.hasNext()) {
                if (child.name.endsWith(".class")) {
                    Resource r = i.next();
                    URI uri = r.url.toURI();
                    files.add(new NodeJavaFileObject(
                            binaryName + "." + child.name.substring(0, child.name.length() - ".class".length()),
                            uri,
                            r.streamFactory,
                            r.lastModified));
                }
            } else {
                if (recurse) {
                    resolve(files, child, binaryName + "." + child.name, recurse);
                }
            }
        }
    }

    public List<JavaFileObject> resolve(String pkg, boolean recurse) throws IOException, URISyntaxException {

        // 如果pkg是个新的(没有包含在前面merge的资源里),在这里从新merge(可能降低性能)
//        mergeIfNessary(pkg);

        Node current = driver.root();

        String[] elts = Utils.split(pkg, '.');

        for (String elt : elts) {
            current = driver.child(current, elt);
            if (current == null) {
                break;
            }
        }

        //
        List<JavaFileObject> files = new ArrayList<JavaFileObject>();
        if (current != null) {
            String binaryName = pkg;
            if (binaryName.startsWith(SPRING_BOOT_CLASS_PREFIX)) {
                binaryName = binaryName.substring(SPRING_BOOT_CLASS_PREFIX.length());
            }
            resolve(files, current, binaryName, recurse);
        }

        // 兼容springboot fat-jar
        if (!pkg.startsWith(SPRING_BOOT_CLASS_PREFIX)) {
            String fatPkg = SPRING_BOOT_CLASS_PREFIX + pkg;
            files.addAll(resolve(fatPkg, recurse));
        }

        return files;
    }

    private void mergeIfNessary(String pkg) throws IOException, URISyntaxException {
        pkg = pkg.replace(".", "/");
        for (String resource : this.resourcePath) {
            if (resource.startsWith(pkg) || pkg.startsWith(resource)) {
                return;
            }
        }

        // 以下自动引入的方式,性能稍差一些
        log.info("--- mergeIfNessary=======pkg={}", pkg);
        this.driver.merge(this.loader, Collections.singleton(pkg));
        this.resourcePath.add(pkg);
    }
}
