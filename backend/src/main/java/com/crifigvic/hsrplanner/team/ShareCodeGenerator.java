package com.crifigvic.hsrplanner.team;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

/**
 * Random base62 codes like {@code aZ3kP9qX}. 8 characters give 62^8 ≈ 2·10^14 combinations,
 * so collisions are rare; {@code TeamService} still retries if one happens.
 * Random instead of an encoded sequence so links can't be enumerated.
 */
@Component
public class ShareCodeGenerator {

    public static final int LENGTH = 8;
    static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final RandomGenerator random;

    public ShareCodeGenerator() {
        this(new SecureRandom());
    }

    ShareCodeGenerator(RandomGenerator random) {
        this.random = random;
    }

    public String next() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    public static boolean isWellFormed(String code) {
        return code != null && code.length() == LENGTH && code.chars().allMatch(c -> ALPHABET.indexOf(c) >= 0);
    }
}
