package com.github.loefberg.nitwit.ds;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class DataStore {
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    private final Path objectsDir;

    public DataStore(Path gitDir) {
        this.objectsDir = gitDir.resolve("objects");
    }

    public String putBlob(Path file) throws IOException {
        long fileSize = Files.size(file);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(String.format("blob %s\0", fileSize).getBytes(StandardCharsets.UTF_8));
        out.write(Files.readAllBytes(file));
        out.close();

        return put(out.toByteArray());
    }

    public String put(byte[] content) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(String.format("blob %s\0", content.length).getBytes(StandardCharsets.UTF_8));
        out.write(content);
        out.close();
        byte[] fileContent = out.toByteArray();
        String hash = hash(fileContent);
        deflate(fileContent, createObjectPath(hash));
        return hash;
    }

    public byte[] get(String key) throws IOException {
        Path objectFile = getObjectPath(key);
        byte[] uncompressed = inflate(objectFile);
        int idx = indexOf(uncompressed, 0);
        String header = new String(uncompressed, 0, idx, StandardCharsets.UTF_8);
        // TODO: handle header
        // TODO: validate hash
        return Arrays.copyOfRange(uncompressed, idx + 1, uncompressed.length);
    }

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

    private Path getObjectPath(String hash) {
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
}
