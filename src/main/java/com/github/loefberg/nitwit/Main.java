package com.github.loefberg.nitwit;

import com.github.loefberg.nitwit.ds.DataStore;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException {
        Path gitDir = Paths.get("D:\\Bitbucket\\modeling-tool\\.git");
        DataStore store = new DataStore(gitDir);
        // byte[] value = store.get("fcbd1a074aae48efa72976015b0ac016c4056dc0"); // commit
        byte[] value = store.get("67fd1bbb4a5db04aa886cfb65cbca2d19f0e7fae"); // tree

        int idx = 0;
        while(value[idx] != 0 && idx < value.length) {
            idx++;
        }

        System.out.println(">");
        System.out.println(new String(value, 0, idx));
    }
}
