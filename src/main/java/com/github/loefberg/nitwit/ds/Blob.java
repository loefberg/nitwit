package com.github.loefberg.nitwit.ds;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Blob {
    private final byte[] content;

    public static Blob createFromFile(Path file) throws IOException {
        return new Blob(Files.readAllBytes(file));
    }

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
