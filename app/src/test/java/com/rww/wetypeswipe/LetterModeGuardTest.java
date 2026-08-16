package com.rww.wetypeswipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public final class LetterModeGuardTest {
    @Test public void englishKeyboardClassNamesAccepted() {
        assertTrue(LetterModeGuard.isEnglishKeyboardClassName(
                "com.tencent.wetype.plugin.hld.keyboard.selfdraw.S3EnglishQwertyKeyboard"));
        assertTrue(LetterModeGuard.isEnglishKeyboardClassName(
                "com.tencent.wetype.plugin.hld.keyboard.selfdraw.S12EnglishNumberSymbolKeyboard"));
        assertFalse(LetterModeGuard.isEnglishKeyboardClassName(
                "com.tencent.wetype.plugin.hld.keyboard.selfdraw.S9DoublePinQwertyKeyboard"));
        assertFalse(LetterModeGuard.isEnglishKeyboardClassName(null));
        assertFalse(LetterModeGuard.isEnglishKeyboardClassName(""));
    }

    @Test public void englishKeyboardTypeNamesAccepted() {
        assertTrue(LetterModeGuard.isEnglishKeyboardTypeName("EnglishQwerty"));
        assertTrue(LetterModeGuard.isEnglishKeyboardTypeName("EnglishNumberSymbols"));
        assertFalse(LetterModeGuard.isEnglishKeyboardTypeName("ChineseQwerty"));
        assertFalse(LetterModeGuard.isEnglishKeyboardTypeName("DoublePinQwerty"));
        assertFalse(LetterModeGuard.isEnglishKeyboardTypeName("Number"));
        assertFalse(LetterModeGuard.isEnglishKeyboardTypeName(""));
        assertFalse(LetterModeGuard.isEnglishKeyboardTypeName(null));
    }

    @Test public void englishKeyboardTypeValuesAccepted() {
        assertTrue(LetterModeGuard.isEnglishKeyboardTypeValue(
                LetterModeGuard.KEYBOARD_TYPE_ENGLISH_QWERTY));
        assertTrue(LetterModeGuard.isEnglishKeyboardTypeValue(
                LetterModeGuard.KEYBOARD_TYPE_ENGLISH_NUMBER_SYMBOLS));
        assertFalse(LetterModeGuard.isEnglishKeyboardTypeValue(0));   // ChineseT9
        assertFalse(LetterModeGuard.isEnglishKeyboardTypeValue(1));   // ChineseQwerty
        assertFalse(LetterModeGuard.isEnglishKeyboardTypeValue(6));   // DoublePinQwerty
        assertFalse(LetterModeGuard.isEnglishKeyboardTypeValue(101)); // Number
        assertFalse(LetterModeGuard.isEnglishKeyboardTypeValue(103)); // ChineseNumberSymbols
        assertFalse(LetterModeGuard.isEnglishKeyboardTypeValue(105)); // PureNumber
    }

    @Test public void touchKeyButtonLookupOrderStable() {
        assertEquals(Arrays.asList("x1", "v1"), LetterModeGuard.touchKeyButtonMethodCandidates());
        assertEquals("B1", LetterModeGuard.coordinateKeyButtonMethod());
    }
}
