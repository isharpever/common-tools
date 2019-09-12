package com.isharpever.tool.dynamic.compile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import org.crsh.vfs.spi.AbstractFSDriver;

public class URLDriver extends AbstractFSDriver<Node> {

    /** . */
    private final Node root;

    public URLDriver() {
        this.root = new Node();
    }

    public URLDriver merge(ClassLoader loader, Set<String> resourcePath) throws IOException, URISyntaxException {
        root.merge(loader, resourcePath);
        return this;
    }

    public URLDriver merge(URL url) throws IOException, URISyntaxException {
        root.mergeEntries(url);
        return this;
    }

    @Override
    public Node root() throws IOException {
        return root;
    }

    @Override
    public String name(Node handle) throws IOException {
        return handle.name;
    }

    @Override
    public boolean isDir(Node handle) throws IOException {
        return handle.resources.isEmpty();
    }

    @Override
    public Iterable<Node> children(Node handle) throws IOException {
        return handle.children();
    }

    @Override
    public long getLastModified(Node handle) throws IOException {
        return handle.resources.isEmpty() ? 0 : handle.resources.peekFirst().lastModified;
    }

    @Override
    public Iterator<InputStream> open(Node handle) throws IOException {
        ArrayList<InputStream> list = new ArrayList<InputStream>(handle.resources.size());
        for (Resource resource : handle.resources) {
            list.add(resource.streamFactory.open());
        }
        return list.iterator();
    }
}
