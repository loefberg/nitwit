package com.github.loefberg.nitwit;

public enum ObjectType {
    REGULAR_FILE(0b01000),
    SYMBOLIC_LINK(0b1010),
    /**
     * A submodule. Points to a sha1 of the submodule repo.
     */
    GIT_LINK(0b1110);

    private final int value;
    private ObjectType(int value) {
        this.value = value;
    }

    public static ObjectType fromValue(byte value) {
        switch(value) {
            case 0b1000: return REGULAR_FILE;
            case 0b1010: return SYMBOLIC_LINK;
            case 0b1110: return GIT_LINK;
        }

        throw new IllegalArgumentException("Unknown object type value: " + value);
    }
}
