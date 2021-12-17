package com.github.loefberg.nitwit.ds;

import java.io.IOException;
import java.io.OutputStream;

public interface Entry {
    long size();
    String type();
    void write(OutputStream out) throws IOException;
}
