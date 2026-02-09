package com.hotel.testdata;

import java.util.UUID;

public final class TestDataUtils {

    private TestDataUtils() {
    }

    public static String randomCodigo(String prefix) {
        if (prefix == null) {
            prefix = "";
        }
        if (prefix.length() > 20) {
            throw new IllegalArgumentException("prefix excede 20 caracteres");
        }
        String base = UUID.randomUUID().toString().replace("-", "");
        int maxLen = 20 - prefix.length();
        return prefix + base.substring(0, maxLen);
    }
}
