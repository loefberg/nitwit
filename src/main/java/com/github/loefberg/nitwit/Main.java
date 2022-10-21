package com.github.loefberg.nitwit;

import com.github.loefberg.nitwit.ds.DataStore;
import com.github.loefberg.nitwit.ds.Tree;
import com.github.loefberg.nitwit.ds.Tree.TreeEntry;
import com.github.loefberg.nitwit.ds.TreeFileType;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException {
        Path gitDir = Paths.get("D:\\Bitbucket\\modeling-tool\\.git");
        DataStore ds = new DataStore(gitDir);

        System.out.println(ds.getType(new ObjectID("de49e2008450cb78443ead2c547683406990b08b")));
        Tree tree = ds.getTree(new ObjectID("de49e2008450cb78443ead2c547683406990b08b"));
        for(TreeEntry entry: tree.getEntries()) {
            System.out.println(entry);
            if(entry.type() == TreeFileType.DIRECTORY) {
                ds.getTree(entry.id());
            }
        }
        // 040000 tree 4921cb7ea1e8c3326853409ec53cee891e61e671    main
    }
}
