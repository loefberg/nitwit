package com.github.loefberg.nitwit.ds;

import com.github.loefberg.nitwit.ObjectID;
import com.github.loefberg.nitwit.ds.Tree.TreeEntry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class DataStoreTest {
    private static FileSystem FS = null;
    private static Path GIT_DIR = null;

    @BeforeAll
    static void beforeAll() throws IOException {
        Path zipWithRepo = Paths.get("src/test/resources/simple.zip").toAbsolutePath();
        if(!Files.exists(zipWithRepo)) {
            throw new FileNotFoundException("Zip file with test git repository not found: " + zipWithRepo);
        }
        FS = FileSystems.newFileSystem(zipWithRepo, DataStoreTest.class.getClassLoader());
        GIT_DIR = FS.getPath(".git");
    }

    @Test
    public void testGetBlobEmpty() throws Exception {
        DataStore ds = new DataStore(GIT_DIR);
        Blob b = ds.getBlob(new ObjectID("e69de29bb2d1d6434b8b29ae775ad8c2e48c5391"));
        assertEquals(0, b.getSize());
        assertArrayEquals(new byte[0], b.getContent());
    }

    @Test
    public void testGetBlobTextFile() throws Exception {
        DataStore ds = new DataStore(GIT_DIR);
        Blob b = ds.getBlob(new ObjectID("af5626b4a114abcb82d63db7c8082c3c4756e51b"));
        assertEquals("Hello, world!\n", new String(b.getContent(), StandardCharsets.UTF_8));
    }

    @Test
    public void testGetBlobSymLink() throws Exception {
        DataStore ds = new DataStore(GIT_DIR);
        Blob b = ds.getBlob(new ObjectID("a5162f80d4a6782b7cb2a0a197f834e683cb9eb1"));
        assertEquals("hello.txt", new String(b.getContent(), StandardCharsets.UTF_8));
    }

    //@Test
    public void testGetBlobBinary() throws Exception {
        fail("Not yet implemented");
    }

    //@Test
    public void getCommit() throws Exception {
        DataStore ds = new DataStore(GIT_DIR);
        // Commit c = ds.getCommit(new ObjectID("c815bda30d2eab42be2ff89bc01d2e5c4752cab5"));
        fail("Not yet implemented");
    }

    /**
     * Test an empty tree.
     *
     * This was created with:
     * git init
     * git write-tree
     */
    @Test
    public void testEmptyTree() throws Exception {
        Path zipWithRepo = Paths.get("src/test/resources/empty-tree.zip").toAbsolutePath();
        if(!Files.exists(zipWithRepo)) {
            throw new FileNotFoundException("Zip file with test git repository not found: " + zipWithRepo);
        }
        FileSystem fs = FileSystems.newFileSystem(zipWithRepo, DataStoreTest.class.getClassLoader());
        Path gitDir = fs.getPath(".git");
        DataStore ds = new DataStore(gitDir);
        Tree tree = ds.getTree(new ObjectID("4b825dc642cb6eb9a060e54bf8d69288fbee4904"));
        assertEquals(0, tree.getEntries().size());
    }

    @Test
    public void testTree() throws Exception {
        DataStore ds = new DataStore(GIT_DIR);
        Tree tree = ds.getTree(new ObjectID("4f83f3fcc9fc67e784d0348900c420b0a3bee799"));
        List<TreeEntry> entries = tree.getEntries();
        assertEquals(6, entries.size());

        int idx = 0;
        assertEquals("5b253266d4e1848ce479c78d8db7228e5d8b5ee7", entries.get(idx).id().getHashString());
        assertEquals("dir1", entries.get(idx).name());
        assertEquals(Integer.parseInt("000", 8), entries.get(idx).permissions());
        assertEquals(TreeFileType.DIRECTORY, entries.get(idx).type());

        idx = 1;
        assertEquals("14c73be0322647ab91da4dc4e97d290f551a5d64", entries.get(idx).id().getHashString());
        assertEquals("dir2", entries.get(idx).name());
        assertEquals(Integer.parseInt("000", 8), entries.get(idx).permissions());
        assertEquals(TreeFileType.DIRECTORY, entries.get(idx).type());

        idx = 2;
        assertEquals("e69de29bb2d1d6434b8b29ae775ad8c2e48c5391", entries.get(idx).id().getHashString());
        assertEquals("empty.txt", entries.get(idx).name());
        assertEquals(Integer.parseInt("644", 8), entries.get(idx).permissions());
        assertEquals(TreeFileType.REGULAR_FILE, entries.get(idx).type());

        idx = 3;
        assertEquals("af5626b4a114abcb82d63db7c8082c3c4756e51b", entries.get(idx).id().getHashString());
        assertEquals("hello.txt", entries.get(idx).name());
        assertEquals(Integer.parseInt("644", 8), entries.get(idx).permissions());
        assertEquals(TreeFileType.REGULAR_FILE, entries.get(idx).type());

        idx = 4;
        assertEquals("4bf669a28791d113245ac7813b0af1cc979d023e", entries.get(idx).id().getHashString());
        assertEquals("symlink-to-dir2-file05.bin", entries.get(idx).name());
        assertEquals(Integer.parseInt("000", 8), entries.get(idx).permissions());
        assertEquals(TreeFileType.SYMBOLIC_LINK, entries.get(idx).type());

        idx = 5;
        assertEquals("a5162f80d4a6782b7cb2a0a197f834e683cb9eb1", entries.get(idx).id().getHashString());
        assertEquals("symlink-to-hello.txt", entries.get(idx).name());
        assertEquals(Integer.parseInt("000", 8), entries.get(idx).permissions());
        assertEquals(TreeFileType.SYMBOLIC_LINK, entries.get(idx).type());
    }
}