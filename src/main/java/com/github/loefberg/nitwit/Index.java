package com.github.loefberg.nitwit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static com.github.loefberg.nitwit.ByteBufferUtils.getEpochSecond;
import static com.github.loefberg.nitwit.ByteBufferUtils.getUnsignedInt;

public class Index {
    private static final int MAGIC = ('D' << 24) | ('I' << 16) | ('R' << 8) | ('C' << 0);

    public static void main(String[] args) throws IOException {
        Path file = Paths.get("D:\\Bitbucket\\modeling-tool\\.git\\index");
        byte[] content = Files.readAllBytes(file);
        ByteBuffer buf = ByteBuffer.wrap(content);

        // 4-byte signature:
        // The signature is { 'D', 'I', 'R', 'C' } (stands for "dircache")
        int signature = buf.getInt();
        if(signature != MAGIC) {
            throw new RuntimeException("Invalid index file format, magic signature wrong: " + signature);
        }


        // 4-byte version number:
        // The current possible versions are 2, 3 and 4.
        int version = buf.getInt();
        System.out.println("Version=" + version);

        if(version != 2) {
            throw new RuntimeException("Unsupported index version=" + version);
        }

        // 32-bit number of index entries.
        long numberOfEntries = getUnsignedInt(buf);
        System.out.println("entries=" + numberOfEntries);

        // entries

        for(long i = 0; i < numberOfEntries; i++) {
            //  Index entries are sorted in ascending order on the name field,
            //  interpreted as a string of unsigned bytes (i.e. memcmp() order, no
            //  localization, no special casing of directory separator '/'). Entries
            //  with the same name are sorted by their stage field.

            // 32-bit ctime seconds, the last time a file's metadata changed
            //    this is stat(2) data
            //  32-bit ctime nanosecond fractions
            //    this is stat(2) data
            Instant ctime = getEpochSecond(buf);

            //  32-bit mtime seconds, the last time a file's data changed
            //    this is stat(2) data
            //
            //  32-bit mtime nanosecond fractions
            //    this is stat(2) data

            Instant mtime = getEpochSecond(buf);

            //  32-bit dev
            //    this is stat(2) data
            long dev = getUnsignedInt(buf);

            //
            //  32-bit ino
            //    this is stat(2) data
            long ino = getUnsignedInt(buf);

            //  32-bit mode, split into (high to low bits)
            long mode = getUnsignedInt(buf);
            System.out.println(Integer.toBinaryString((int)mode));
            // mode is split up like this:
            //
            //    4-bit object type
            //      valid values in binary are 1000 (regular file), 1010 (symbolic link)
            //      and 1110 (gitlink)
            ObjectType objectType = ObjectType.fromValue((byte)((mode & 0xf000) >> 12));

            //    3-bit unused
            //
            // 1ff
            //    9-bit unix permission. Only 0755 and 0644 are valid for regular files.
            //    Symbolic links and gitlinks have value 0 in this field.
            short permissions = (short)(mode & 0x1ff);


            //  32-bit uid
            //    this is stat(2) data
            long uid = getUnsignedInt(buf);

            //
            //  32-bit gid
            //    this is stat(2) data
            long gid = getUnsignedInt(buf);

            //
            //  32-bit file size
            //    This is the on-disk size from stat(2), truncated to 32-bit.
            long size = getUnsignedInt(buf);


            //  160-bit SHA-1 for the represented object
            byte[] hash = new byte[20];
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

            // System.out.println(entryPathName);
        }
    }
}
