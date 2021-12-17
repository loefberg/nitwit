package com.github.loefberg.nitwit.ds;

import com.github.loefberg.nitwit.ObjectID;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tree {
    private final List<TreeEntry> entries = new ArrayList<>();

    public Tree(byte[] content) {
        for(int idx = 0; idx < content.length;) {
            int modeOffset = idx;
            int modeLength = 0;

            while (content[modeOffset + modeLength] != ' ') {
                modeLength++;
            }

            long mode = Long.parseLong(new String(content, modeOffset, modeLength, StandardCharsets.UTF_8), 8);

            int nameOffset = modeOffset + modeLength + 1;
            int nameLength = 0;
            while (content[nameOffset + nameLength] != 0) {
                nameLength++;
            }

            String name = new String(content, nameOffset, nameLength, StandardCharsets.UTF_8);

            int hashFrom = nameOffset + nameLength + 1;
            int hashTo = hashFrom + 20;

            ObjectID id = new ObjectID(Arrays.copyOfRange(content, hashFrom, hashTo));

            entries.add(new TreeEntry(mode, name, id));

            idx = hashTo;
        }
    }

    public List<TreeEntry> getEntries() {
        return entries;
    }

    public static class TreeEntry {
        private final long mode;
        private final TreeFileType type;
        private final short permissions;
        private final String name;
        private final ObjectID id;

        public TreeEntry(long mode, String name, ObjectID id) {
            this.mode = mode;
            this.type = TreeFileType.fromValue((byte)((mode & 0xf000) >> 12));
            //    9-bit unix permission. Only 0755 and 0644 are valid for regular files.
            //    Symbolic links and gitlinks have value 0 in this field.
            this.permissions = (short)(mode & 0x1ff);

            this.name = name;
            this.id = id;
        }

        public ObjectID id() {
            return id;
        }

        public TreeFileType type() {
            return type;
        }

        public String name() {
            return name;
        }

        public short permissions() {
            return permissions;
        }

        @Override
        public String toString() {
            return String.format("%06o %s %s    %s", mode, getTypeColumn(), id.getHashString(), name);
        }

        private String getTypeColumn() {
            switch(type) {
                case DIRECTORY: return "tree";
                case REGULAR_FILE: return "blob";
                case SYMBOLIC_LINK: return "blob";
            }
            throw new RuntimeException("Not yet implemented: " + type);
        }
    }
}
