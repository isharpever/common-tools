package com.isharpever.tool.dynamic.compile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import org.apache.commons.collections4.CollectionUtils;
import org.crsh.util.InputStreamFactory;
import org.crsh.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Node implements Iterable<Resource> {
    private static final Logger log = LoggerFactory.getLogger(Node.class);

    /** . */
    private static final File[] EMPTY = new File[0];

    /** . */
    public final String name;

    /** The lazy dires not yet processed. */
    File[] dirs = EMPTY;

    /** . */
    HashMap<String, Node> children = new HashMap<String, Node>();

    /** . */
    LinkedList<Resource> resources = new LinkedList<Resource>();

    public Node() {
        this.name = "";
    }

    private Node(String name) {
        this.name = name;
    }

    void merge(ClassLoader loader, Set<String> resourcePath) throws IOException, URISyntaxException {

        // Get the root class path files
        for (Enumeration<URL> i = loader.getResources("");i.hasMoreElements();) {
            URL url = i.nextElement();
            // In some case we can get null (Tomcat 8)
            if (url != null && !url.getProtocol().equals("jar")) {
                mergeEntries(url);
            }
        }
        Set<URL> jarUrls = getResourceJarUrl(loader, resourcePath);
        for (URL item : jarUrls) {
            mergeEntries(item);
        }
    }

    private Set<URL> getResourceJarUrl(ClassLoader loader, Set<String> resourcePath) throws IOException {
        Set<URL> result = new HashSet<>();
        for (String resource : resourcePath) {
            List<URL> items = Collections.list(loader.getResources(resource));
            if (CollectionUtils.isEmpty(items)) {
                continue;
            }
            for (URL item : items) {
                if ("jar".equals(item.getProtocol())) {
                    String path = item.getPath();
                    int pos = path.lastIndexOf("!/");
                    URL url = new URL("jar:" + path.substring(0, pos + 2));
                    result.add(url);
                } else {
                    //
                }
            }
        }
        return result;
    }

    /**
     * Rewrite an URL by analysing the serie of trailing <code>!/</code>. The number of <code>jar:</code> prefixes
     * does not have to be equals to the number of separators.
     *
     * @param url the url to rewrite
     * @return the rewritten URL
     */
    String rewrite(String url) {
        int end = url.lastIndexOf("!/");
        if (end >= 0) {
            String entry = url.substring(end + 2);
            int start = url.indexOf(':');
            String protocol = url.substring(0, start);
            String nestedURL;
            if (protocol.equals("jar")) {
                nestedURL = rewrite(url.substring(start + 1, end));
                return "jar:" + nestedURL + "!/" + entry;
            } else {
                nestedURL = rewrite(url.substring(0, end));
            }
            return "jar:" + nestedURL + "!/" + entry;
        } else {
            return url;
        }
    }

    Iterable<Node> children() throws IOException {
        // Lazy merge the dirs when accessing this node
        // it is not only important for performance reason but in some case
        // the classpath may contain an exploded dir that see the the whole file system
        // and the full scan is an issue
        while (true) {
            int length = dirs.length;
            if (length > 0) {
                File dir = dirs[length - 1];
                dirs = Arrays.copyOf(dirs, length - 1);
                merge(dir);
            } else {
                break;
            }
        }
        return children.values();
    }

    public void mergeEntries(URL url) throws IOException, URISyntaxException {
        // We handle a special case of spring-boot URLs here before diving in the recursive analysis
        // see https://github.com/spring-projects/spring-boot/tree/master/spring-boot-tools/spring-boot-loader#urls
        if (url.getProtocol().equals("jar")) {
            url = new URL(rewrite(url.toString()));
        }
        _mergeEntries(url);
    }

    private void _mergeEntries(URL url) throws IOException, URISyntaxException {
        if (url.getProtocol().equals("file")) {
            try {
                File f = Utils.toFile(url);
                if (f.isDirectory()) {
                    merge(f);
                } else if (f.getName().endsWith(".jar")) {
                    mergeEntries(new URL("jar:" + url + "!/"));
                } else {
                    // WTF ?
                }
            }
            catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }
        else if (url.getProtocol().equals("jar")) {
            int pos = url.getPath().lastIndexOf("!/");
            URL jarURL = new URL(url.getPath().substring(0, pos));
            String path = url.getPath().substring(pos + 2);
            ZipIterator i = ZipIterator.create(jarURL);
            try {
                while (i.hasNext()) {
                    ZipEntry entry = i.next();
                    if (entry.getName().startsWith(path)) {
                        addEntry(url, entry.getName().substring(path.length()), i.getStreamFactory());
                    }
                }
            }
            finally {
                Utils.close(i);
            }
        }
        else {
            if (url.getPath().endsWith(".jar")) {
                mergeEntries(new URL("jar:" + url + "!/"));
            } else {
                // WTF ?
            }
        }
    }

    private void merge(File f) throws IOException {
        File[] files = f.listFiles();
        if (files != null) {
            for (final File file : files) {
                String name = file.getName();
                Node child = children.get(name);
                if (file.isDirectory()) {
                    if (child == null) {
                        child = new Node(name);
                        children.put(name, child);
                    }
                    int length = child.dirs.length;
                    child.dirs = Arrays.copyOf(child.dirs, length + 1);
                    child.dirs[length] = file;
                } else {
                    if (child == null) {
                        children.put(name, child = new Node(name));
                    }
                    child.resources.add(
                            new Resource(file.toURI().toURL(),
                                    new InputStreamFactory() {
                                        public InputStream open() throws IOException {
                                            return new FileInputStream(file);
                                        }
                                    }, file.lastModified()
                            )
                    );
                }
            }
        }
    }

    private void addEntry(URL baseURL, String entryName, InputStreamFactory resolver) throws IOException {
        if (entryName.length() > 0 && entryName.charAt(entryName.length() - 1) != '/') {
            addEntry(baseURL, 0, entryName, 1, resolver);
        }
    }

    private void addEntry(URL baseURL, int index, String entryName, long lastModified, InputStreamFactory resolver) throws IOException {
        int next = entryName.indexOf('/', index);
        if (next == -1) {
            String name = entryName.substring(index);
            Node child = children.get(name);
            if (child == null) {
                children.put(name, child = new Node(name));
            }
            child.resources.add(new Resource(new URL(baseURL + entryName), resolver, lastModified));
        }
        else {
            String name = entryName.substring(index, next);
            Node child = children.get(name);
            if (child == null) {
                children.put(name, child = new Node(name));
            }
            child.addEntry(baseURL, next + 1, entryName, lastModified, resolver);
        }
    }

    @Override
    public Iterator<Resource> iterator() {
        return resources.iterator();
    }
}
