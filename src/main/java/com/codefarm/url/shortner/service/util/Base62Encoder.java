package com.codefarm.url.shortner.service.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Short answer: Variable-length. With your current Snowflake + Base62 approach it’s not fixed.
     * Practical with your generator:
     * Today (2025-10): typically 10 characters.
     * It will switch to 11 characters around 2027 (~6.3 years after 2021-01-01 epoch start).
     * It will stay ≤11 chars for the lifetime of the Snowflake epoch (41-bit timestamp); it won’t reach 12.
     * Theoretical bounds of toBase62(long) itself:
     * Minimum 1 char (for 0).
     * 6 chars cover up to 62^6−1 ≈ 5.68e10.
     * 10 chars cover up to 62^10−1 ≈ 8.39e17.
     * 11 chars cover up to 62^11−1 ≈ 5.20e19.
     * @param number
     * @return
     */
    public String toBase62(long number) {
        if (number == 0) return "0";
        StringBuilder builder = new StringBuilder();
        while (number > 0) {
            int idx = (int) (number % 62);
            builder.append(ALPHABET.charAt(idx));
            number = number / 62;
        }
        return builder.reverse().toString();
    }

    public long fromBase62(String code) {
        long result = 0;
        for (int i = 0; i < code.length(); i++) {
            int val = ALPHABET.indexOf(code.charAt(i));
            if (val < 0) throw new IllegalArgumentException("Invalid base62 character: " + code.charAt(i));
            result = result * 62 + val;
        }
        return result;
    }
}


