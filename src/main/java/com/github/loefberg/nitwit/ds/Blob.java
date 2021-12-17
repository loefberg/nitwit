package com.github.loefberg.nitwit.ds;

import java.io.IOException;
import java.io.OutputStream;

public class Blob {
    private final byte[] content;

    public Blob(byte[] content) {
        this.content = content;
    }

    public long getSize() {
        return content.length;
    }

    public void write(OutputStream out) throws IOException {
        out.write(content);
    }

    public byte[] getContent() {
        return content;
    }
}
