package com.isharpever.tool.dynamic.compile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import org.crsh.util.InputStreamFactory;

class NodeJavaFileObject implements JavaFileObject {

    /** . */
    final String binaryName;

    /** . */
    private final URI uri;

    /** . */
    private final String name;

    /** . */
    private final long lastModified;

    /** . */
    private final InputStreamFactory stream;

    public NodeJavaFileObject(String binaryName, URI uri, InputStreamFactory stream, long lastModified) {
        this.uri = uri;
        this.binaryName = binaryName;
        this.name = uri.getPath() == null ? uri.getSchemeSpecificPart() : uri.getPath();
        this.lastModified = lastModified;
        this.stream = stream;
    }

    @Override
    public URI toUri() {
        return uri;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return stream.open();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Writer openWriter() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public Kind getKind() {
        return Kind.CLASS;
    }

    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        String baseName = simpleName + kind.extension;
        return kind.equals(getKind())
                && (baseName.equals(getName())
                || getName().endsWith("/" + baseName));
    }

    @Override
    public NestingKind getNestingKind() {
        return null;
    }

    @Override
    public Modifier getAccessLevel() {
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[uri=" + uri + "]";
    }
}