package com.github.loefberg.nitwit;

import java.io.IOException;
import java.nio.file.Path;

public class Nitwit {
    public Workspace createWorkspace(Path dir) throws IOException {
        return Workspace.create(dir);
    }
}
