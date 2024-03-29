package com.github.loefberg.nitwit;

import com.github.loefberg.nitwit.ds.Blob;
import com.github.loefberg.nitwit.ds.DataStore;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Workspace {
    public static Workspace create(Path dir) throws IOException {
        Path gitDir = dir.resolve(".git");
        Files.createDirectory(gitDir);
        Files.createDirectory(gitDir.resolve("info"));
        Files.createDirectory(gitDir.resolve("objects"));
        Files.createDirectory(gitDir.resolve("objects/info"));
        Files.createDirectory(gitDir.resolve("objects/pack"));
        Files.createDirectory(gitDir.resolve("refs"));
        Files.createDirectory(gitDir.resolve("refs/heads"));
        Files.createDirectory(gitDir.resolve("refs/tags"));

        copyResource("/template/config", gitDir.resolve("config"));
        copyResource("/template/description", gitDir.resolve("description"));
        copyResource("/template/HEAD", gitDir.resolve("HEAD"));

        // To ignore untracked files, you have a file in your git folder called . git/info/exclude . This file is your
        // own gitignore inside your local git folder, which means is not going to be committed or shared with anyone
        // else. You can basically edit this file and stop tracking any (untracked) file.
        copyResource("/template/info/exclude", gitDir.resolve("info/exclude"));

        return new Workspace(dir);
    }

    private final DataStore ds;

    public Workspace(Path workspace) {
        this.ds = new DataStore(workspace.resolve(".git"));
    }

    public String createObject(Path file) throws IOException {
        Blob blob = Blob.createFromFile(file);
        return ds.putBlob(blob);
    }

    private static void copyResource(String name, Path target) throws IOException {
        try(InputStream input = Workspace.class.getResourceAsStream(name);
            OutputStream out = new FileOutputStream(target.toFile())) {

            byte[] buf = new byte[1024];
            int read;
            while((read = input.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        }
    }
}
