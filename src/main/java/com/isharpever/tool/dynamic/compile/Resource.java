package com.isharpever.tool.dynamic.compile;

import java.net.URL;
import org.crsh.util.InputStreamFactory;

public class Resource {

    /** . */
    public final URL url;

    /** . */
    public final InputStreamFactory streamFactory;

    /** . */
    public final long lastModified;

    Resource(URL url, InputStreamFactory streamFactory, long lastModified) {
        this.url = url;
        this.streamFactory = streamFactory;
        this.lastModified = lastModified;
    }
}