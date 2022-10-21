package com.github.loefberg.nitwit.ds;

import com.github.loefberg.nitwit.ObjectID;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.Function;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class DataStore {
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    private final Path objectsDir;

    public DataStore(Path gitDir) {
        this.objectsDir = gitDir.resolve("objects");
    }

    public void getObjects() throws IOException {

    }

    public String getType(ObjectID key) throws IOException {
        Path objectFile = requireRegularFile(getObjectPath(key));

        try(var input = new InflaterInputStream(Files.newInputStream(objectFile))) {
            byte[] head = new byte[7];
            int read = input.read(head);
            int length = 0;
            while(length < read && head[length] != ' ') {
                length++;
            }

            return new String(head, 0, length, StandardCharsets.UTF_8);
        }
    }

    public String putBlob(Blob blob) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(String.format("blob %s\0", blob.getSize()).getBytes(StandardCharsets.UTF_8));
        blob.write(out);
        out.close();

        return put(out.toByteArray());
    }

    public Blob getBlob(ObjectID key) throws IOException {
        return get(key, "blob", Blob::new);
    }

    public String putTree(Tree tree) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Tree getTree(ObjectID key) throws IOException {
        return get(key, "tree", Tree::new);
    }

    public Commit getCommit(ObjectID key) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void put(Entry entry) {

    }

    private <T> T get(ObjectID key, String expectedType, Function<byte[], T> ctor) throws IOException {
        Path objectFile = requireRegularFile(getObjectPath(key));
        byte[] uncompressed = inflate(objectFile);
        int idx = indexOf(uncompressed, 0);
        String header = new String(uncompressed, 0, idx, StandardCharsets.UTF_8);
        // Header: tree 612
        // Header: commit 231

        String[] headerColumns = header.split(" ");
        String type = headerColumns[0];
        long length = Long.parseLong(headerColumns[1]);

        if(!type.equals(expectedType)) {
            throw new RuntimeException("Failed to load object, expected '" + expectedType + "' got '" + type + "'");
        }

        // TODO: validate hash

        return ctor.apply(Arrays.copyOfRange(uncompressed, idx + 1, uncompressed.length));
    }

    private String put(byte[] content) throws IOException {
        String hash = hash(content);
        deflate(content, createObjectPath(hash));
        return hash;
    }

//    private byte[] get(String key) throws IOException {
//        Path objectFile = getObjectPath(key);
//        byte[] uncompressed = inflate(objectFile);
//        int idx = indexOf(uncompressed, 0);
//        String header = new String(uncompressed, 0, idx, StandardCharsets.UTF_8);
//        // Header: tree 612
//        // Header: commit 231
//
//        // TODO: nitwit git repo has a6efbc856e924ee5ae364d5aa6cbbec6d4baf61f as a tree
//
//        System.out.println("Header: " + header);
//
//        // TODO: handle header
//        // TODO: validate hash
//        return Arrays.copyOfRange(uncompressed, idx + 1, uncompressed.length);
//    }

    private static String hash(byte[] content) {
        byte[] hash;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            hash = md.digest(content);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        char[] hexChars = new char[hash.length * 2];
        for (int j = 0; j < hash.length; j++) {
            int v = hash[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private Path createObjectPath(String hash) throws IOException {
        Path parent = objectsDir.resolve(hash.substring(0, 2));
        if(!Files.exists(parent)) {
            Files.createDirectory(parent);
        }
        return parent.resolve(hash.substring(2));
    }

    private static void deflate(byte[] content, Path file) throws IOException {
        try(var output = new DeflaterOutputStream(Files.newOutputStream(file))) {
            output.write(content);
        }
    }

    private Path getObjectPath(ObjectID key) {
        String hash = key.getHashString();
        return objectsDir.resolve(hash.substring(0, 2)).resolve(hash.substring(2));
    }

    private static byte[] inflate(Path file) throws IOException {
        try(var input = new InflaterInputStream(Files.newInputStream(file))) {
            return input.readAllBytes();
        }
    }

    private static int indexOf(byte[] arr, int searchElement) {
        for(int i = 0; i < arr.length; i++) {
            if(arr[i] == searchElement) {
                return i;
            }
        }
        return -1;
    }

    private static Path requireRegularFile(Path path) throws IOException {
        if(!Files.exists(path)) {
            throw new FileNotFoundException("Object file not found: " + path);
        }

        if(!Files.isRegularFile(path)) {
            throw new IOException("Object file not regular file: " + path);
        }

        return path;
    }

}
