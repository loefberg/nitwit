package com.github.loefberg.nitwit;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class MainTest {
    private final NativeGit ngit = NativeGit.newInstance();
    private Path nativeWorkingDir;
    private Path implWorkingDir;

    @BeforeEach
    public void initWorkspace() throws Exception {
        nativeWorkingDir = Files.createTempDirectory("nitwit-test-native-");
        implWorkingDir = Files.createTempDirectory("nitwit-test-impl-");
    }

    @AfterEach
    public void cleanupWorkspace() throws Exception {
        FileUtils.deleteDirectory(nativeWorkingDir.toFile());
        FileUtils.deleteDirectory(implWorkingDir.toFile());
    }

    @Test
    public void testInit() throws Exception {
        ngit.run(nativeWorkingDir, "init");
        Nitwit nitwit = new Nitwit();
        nitwit.createWorkspace(implWorkingDir);

        assertSameDirectory();
    }

    @Test
    public void bar() {
        System.out.println("bar");
    }

    private void assertSameDirectory() throws Exception {
        Map<Path, String> nativeFiles = hashGitDir(nativeWorkingDir);
        Map<Path, String> implFiles = hashGitDir(implWorkingDir);

        // all native files not in impl files
        Set<Path> missing = nativeFiles.keySet().stream()
                .filter(Predicate.not(implFiles::containsKey))
                .collect(toSet());

        // all impl files not in native files
        Set<Path> extra = implFiles.keySet().stream()
                .filter(Predicate.not(nativeFiles::containsKey))
                .collect(toSet());

        // if in both impl and native, but different hash
        Set<Path> different = implFiles.keySet().stream()
                .filter(nativeFiles::containsKey)
                .filter(path -> !implFiles.get(path).equals(nativeFiles.get(path)))
                .collect(toSet());

        StringBuilder message = new StringBuilder();
        if(!missing.isEmpty()) {
            message.append("Files in native git's directory, but not ours:\n");
            message.append("\t- ");
            message.append(missing.stream().map(Path::toString).collect(joining("\n\t- ")));
        }

        if(!extra.isEmpty()) {
            message.append("Files in our directory but not native git's:\n");
            message.append("\t- ");
            message.append(extra.stream().map(Path::toString).collect(joining("\n\t- ")));
        }

        if(!different.isEmpty()) {
            message.append("Files in our directory that is not the same as in native git's:\n");
            message.append("\t- ");
            message.append(extra.stream().map(Path::toString).collect(joining("\n\t- ")));
        }

        if(!message.isEmpty()) {
            fail(message.toString());
        }
    }



    private static Map<Path, String> hashGitDir(Path root) throws Exception {
        Map<Path, String> result = new HashMap<>();
        Files.walkFileTree(root, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = root.relativize(dir);
                return relative.equals(Path.of(".git/hooks")) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                result.put(root.relativize(file), hash(file));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    private static String hash(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(Files.readAllBytes(file)));
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

}