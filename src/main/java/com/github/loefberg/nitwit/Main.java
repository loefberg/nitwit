package com.github.loefberg.nitwit;

import com.github.loefberg.nitwit.ds.DataStore;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException {
        Path gitDir = Paths.get("D:\\Bitbucket\\modeling-tool\\.git");
        DataStore store = new DataStore(gitDir);
        byte[] value = store.get("009ecbbda36b97aab61f3b09ce7fa4f555585602");
        System.out.println(new String(value));
    }
}
