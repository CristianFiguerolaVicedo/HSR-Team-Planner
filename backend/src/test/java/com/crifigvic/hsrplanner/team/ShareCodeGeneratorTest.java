package com.crifigvic.hsrplanner.team;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShareCodeGeneratorTest {

    @RepeatedTest(20)
    void generatesWellFormedCodes() {
        String code = new ShareCodeGenerator().next();

        assertEquals(ShareCodeGenerator.LENGTH, code.length());
        assertTrue(ShareCodeGenerator.isWellFormed(code));
    }

    @Test
    void codesDontRepeatInPractice() {
        ShareCodeGenerator generator = new ShareCodeGenerator(new Random(42));
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            codes.add(generator.next());
        }
        assertEquals(10_000, codes.size());
    }

    @Test
    void rejectsMalformedCodes() {
        assertFalse(ShareCodeGenerator.isWellFormed(null));
        assertFalse(ShareCodeGenerator.isWellFormed("short"));
        assertFalse(ShareCodeGenerator.isWellFormed("abc-1234"));
        assertFalse(ShareCodeGenerator.isWellFormed("abcdefghi"));
    }
}
