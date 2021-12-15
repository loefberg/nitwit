package com.github.loefberg.nitwit.util;

import java.math.BigInteger;

public class Hex {
    public static String toHex(byte[] array) {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        return paddingLength > 0
                ? String.format("%0" + paddingLength + "d", 0) + hex
                : hex;
    }
}
