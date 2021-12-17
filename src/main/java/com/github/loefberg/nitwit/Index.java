package com.github.loefberg.nitwit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.loefberg.nitwit.util.ByteBufferUtils.getEpochSecond;
import static com.github.loefberg.nitwit.util.ByteBufferUtils.getUnsignedInt;
import static com.github.loefberg.nitwit.util.Hex.toHex;

public class Index {
    private static final int MAGIC = ('D' << 24) | ('I' << 16) | ('R' << 8) | ('C' << 0);
    private final int version;
    private List<IndexEntry> entries = new ArrayList<>();

    public static void main(String[] args) throws IOException {

        Path file = Paths.get("D:\\Bitbucket\\modeling-tool\\.git\\index");
        byte[] content = Files.readAllBytes(file);
        ByteBuffer buf = ByteBuffer.wrap(content);
        Index index = new Index(buf);

        // index.entries.forEach(e -> System.out.println(e));
    }

    public Index(ByteBuffer buf) {
        // 4-byte signature:
        // The signature is { 'D', 'I', 'R', 'C' } (stands for "dircache")
        int signature = buf.getInt();
        if(signature != MAGIC) {
            throw new RuntimeException("Invalid index file format, magic signature wrong: " + signature);
        }

        // 4-byte version number:
        // The current possible versions are 2, 3 and 4.
        version = buf.getInt();
        if(version != 2) {
            throw new RuntimeException("Unsupported index version=" + version);
        }

        // 32-bit number of index entries.
        long numberOfEntries = getUnsignedInt(buf);

        // entries
        for(long i = 0; i < numberOfEntries; i++) {
            entries.add(new IndexEntry(buf));
        }

        // TODO: extensions

        buf.position(buf.limit() - 20);
        byte[] expectedHashSum = new byte[20];
        buf.get(expectedHashSum);


        buf.rewind();
        ByteBuffer window = buf.slice().limit(buf.remaining() - 20);
        byte[] actualHashSum;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(window);
            actualHashSum = digest.digest();
        } catch(NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }

        if(!Arrays.equals(expectedHashSum, actualHashSum)) {
            throw new RuntimeException("index file has invalid checksum, expected=" + toHex(expectedHashSum) +
                    ", was=" + toHex(actualHashSum));
        }
    }

    public static class IndexEntry {
        /** The last time a file's metadata changed. */
        private final Instant ctime;

        /** The last time a file's data changed. */
        private final Instant mtime;

        /**
         * Within a POSIX system, this is the device ID (this identifies the device containing the file; that is, the
         * scope of uniqueness of the serial number).
         */
        private final long dev;

        /** Within a POSIX system, this is the inode (index node); the file's "serial number". */
        private final long ino;

        private final IndexFileType objectType;

        /**
         * Unix permission. Only 0755 and 0644 are valid for regular files.
         * Symbolic links and gitlinks have value 0 in this field.
         */
        private final short permissions;

        /** Within a POSIX system, this is the user ID of the file's owner. */
        private final long uid;

        /** Within a POSIX system, this is the group ID of the file. */
        private final long gid;

        /** This is the on-disk size from stat(2), truncated to 32-bit. */
        private final long size;

        /** 160-bit SHA-1 for the represented object */
        private final byte[] hash;

        /**
         * When the "assume unchanged" bit is on, the user promises not to change the file and allows Git to assume
         * that the working tree file matches what is recorded in the index. If you want to change the working tree
         * file, you need to unset the bit to tell Git. This is sometimes helpful when working with a big project
         * on a filesystem that has very slow lstat(2) system call (e.g. cifs).
         */
        private final boolean assumeUnchanged;

        /**
         * Determines if the extended flags are present or not. Must be 0 on version 2 which does not have
         * extended flags.
         */
        private final boolean extended;

        /***
         * 2-bit stage (during merge).
         * 0: regular file, not in a merge conflict
         * 1: base, the common ancestor
         * 2: ours, the target (HEAD) version
         * 3: theirs, the being-merged-in version.
         */
        private final short stage;

        /**
         * 12-bit name length if the length is less than 0xFFF; otherwise 0xFFF is stored in this field.
         */
        private final short nameLength;

        // Entry path name (variable length) relative to top level directory
        //    (without leading slash). '/' is used as path separator. The special
        //    path components ".", ".." and ".git" (without quotes) are disallowed.
        //    Trailing slash is also disallowed.
        //
        //    The exact encoding is undefined, but the '.' and '/' characters
        //    are encoded in 7-bit ASCII and the encoding cannot contain a NUL
        //    byte (iow, this is a UNIX pathname).
        private final String pathName;

        public IndexEntry(ByteBuffer buf) {
            //  Index entries are sorted in ascending order on the name field,
            //  interpreted as a string of unsigned bytes (i.e. memcmp() order, no
            //  localization, no special casing of directory separator '/'). Entries
            //  with the same name are sorted by their stage field.

            // 32-bit ctime seconds, the last time a file's metadata changed
            //    this is stat(2) data
            //  32-bit ctime nanosecond fractions
            //    this is stat(2) data
            this.ctime = getEpochSecond(buf);

            //  32-bit mtime seconds, the last time a file's data changed
            //    this is stat(2) data
            //
            //  32-bit mtime nanosecond fractions
            //    this is stat(2) data
            this.mtime = getEpochSecond(buf);

            //  32-bit dev
            //    this is stat(2) data
            this.dev = getUnsignedInt(buf);

            //
            //  32-bit ino
            //    this is stat(2) data
            this.ino = getUnsignedInt(buf);

            //  32-bit mode, split into (high to low bits)
            long mode = getUnsignedInt(buf);

            // mode is split up like this:
            //
            //    4-bit object type
            //      valid values in binary are 1000 (regular file), 1010 (symbolic link)
            //      and 1110 (git link)
            this.objectType = IndexFileType.fromValue((byte)((mode & 0xf000) >> 12));

            //    3-bit unused
            //
            // 1ff
            //    9-bit unix permission. Only 0755 and 0644 are valid for regular files.
            //    Symbolic links and gitlinks have value 0 in this field.
            this.permissions = (short)(mode & 0x1ff);


            //  32-bit uid
            //    this is stat(2) data
            this.uid = getUnsignedInt(buf);

            //
            //  32-bit gid
            //    this is stat(2) data
            this.gid = getUnsignedInt(buf);

            //
            //  32-bit file size
            //    This is the on-disk size from stat(2), truncated to 32-bit.
            this.size = getUnsignedInt(buf);


            //  160-bit SHA-1 for the represented object
            this.hash = new byte[20];
            buf.get(hash);

            //
            //  A 16-bit 'flags' field split into (high to low bits)
            //
            //    1-bit assume-valid flag
            //
            //    1-bit extended flag (must be zero in version 2)
            //
            //    2-bit stage (during merge)
            //
            //    12-bit name length if the length is less than 0xFFF; otherwise 0xFFF
            //    is stored in this field.
            int flags = buf.getShort();
            this.assumeUnchanged = ((flags & 0b1000000000000000) != 0);
            this.extended = ((flags & 0b0100000000000000) != 0);
            this.stage = (short)((flags & 0b0011000000000000) >> 12);
            this.nameLength = (short)(flags & 0xfff);

            // Entry path name (variable length) relative to top level directory
            //    (without leading slash). '/' is used as path separator. The special
            //    path components ".", ".." and ".git" (without quotes) are disallowed.
            //    Trailing slash is also disallowed.
            //
            //    The exact encoding is undefined, but the '.' and '/' characters
            //    are encoded in 7-bit ASCII and the encoding cannot contain a NUL
            //    byte (iow, this is a UNIX pathname).

            StringBuilder entryPathName = new StringBuilder();
            byte ch;
            while ((ch = buf.get()) != 0) {
                entryPathName.append((char) ch);
            }

            //  1-8 nul bytes as necessary to pad the entry to a multiple of eight bytes
            //  while keeping the name NUL-terminated.
            while (buf.get(buf.position()) == 0) {
                buf.get();
            }

            this.pathName = entryPathName.toString();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder .append("  File: ").append(pathName).append('\n')
                    .append("  Size: ").append(size).append('\t').append(objectType).append('\n')
                    .append("Device: ").append(dev).append('\t')
                        .append("Inode: ").append(ino).append('\n')
                    .append("Access: ").append(Integer.toOctalString(permissions)).append('\t')
                        .append("Uid: ").append(uid).append('\t')
                        .append("Gid: ").append(gid).append('\n')
                    .append("Modify: ").append(mtime).append('\n')
                    .append("Change: ").append(ctime).append('\n')
                    .append("Assume-unchanged: ").append(assumeUnchanged).append(", ")
                        .append("Extended: ").append(extended).append(", ")
                        .append("Stage: ").append(stage).append('\n')
                    .append("Hash: ").append(toHex(hash));

            return builder.toString();
        }
    }
}
