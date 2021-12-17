package com.github.loefberg.nitwit;

import com.github.loefberg.nitwit.util.Hex;

import java.util.Objects;

public class ObjectID {
    private final String hash;

    public ObjectID(String hash) {
        this.hash = hash;
    }

    public ObjectID(byte[] hash) {
        if(Objects.requireNonNull(hash).length != 20) {
            throw new IllegalArgumentException("Invalid hash length: " + hash.length);
        }
        this.hash = Hex.toHex(hash);
    }

    public String getHashString() {
        return hash;
    }
}
