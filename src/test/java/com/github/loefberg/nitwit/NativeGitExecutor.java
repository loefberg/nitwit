package com.github.loefberg.nitwit;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class NativeGitExecutor {
    public static void main(String[] args) throws IOException, InterruptedException {
        runGit(Paths.get("D:\\Documents\\tmp\\git\\test2"), List.of(List.of("git", "init")));
        clean(Paths.get("D:\\Documents\\tmp\\git\\test2"));
    }

    public static void runGit(Path workingDirectory, List<List<String>> commands) throws IOException, InterruptedException {
        for(List<String> command: commands) {
            ProcessBuilder pb = new ProcessBuilder(command)
                    .directory(workingDirectory.toFile());

            Process process = pb.start();

            if(process.waitFor() != 0) {
                throw new IOException("Command exited with non zero exit value");
            }
        }
    }

    public static void clean(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw exc;
                }
            }
        });
    }
}
