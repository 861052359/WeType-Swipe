package com.rww.wetypeswipe;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 字母模式（英文键盘逐字母直接上屏、不显示候选词）的稳定特征判定。
 *
 * <p>微信输入法内部类名与 KeyboardType 枚举名会随版本变化，集中在这里管理，
 * 便于单元测试锁定识别规则，避免在主 Hook 里散落硬编码字符串。</p>
 */
final class LetterModeGuard {
    private LetterModeGuard() {}

    /** 3.5.x KeyboardType 枚举中英文键盘的取值。 */
    static final int KEYBOARD_TYPE_ENGLISH_QWERTY = 100;
    static final int KEYBOARD_TYPE_ENGLISH_NUMBER_SYMBOLS = 102;

    /**
     * 英文键盘类名特征：3.5.x 自绘英文键盘为
     * S3EnglishQwertyKeyboard / S12EnglishNumberSymbolKeyboard，
     * 后续新增英文键盘也会沿用 English 字样。
     */
    static boolean isEnglishKeyboardClassName(String className) {
        if (className == null || className.isEmpty()) return false;
        return className.toLowerCase(Locale.ROOT).contains("english");
    }

    /**
     * KeyboardType 枚举名特征（jadx 从 Kotlin 元数据还原的稳定名称；
     * 运行时名称若被混淆则本方法不可用，由枚举值兜底）。
     */
    static boolean isEnglishKeyboardTypeName(String name) {
        if (name == null || name.isEmpty()) return false;
        return "EnglishQwerty".equals(name) || "EnglishNumberSymbols".equals(name);
    }

    /** KeyboardType 枚举值特征：只接受英文键盘两个取值，其余（含中文/双拼/手写/数字符号）一律不适用。 */
    static boolean isEnglishKeyboardTypeValue(int value) {
        return value == KEYBOARD_TYPE_ENGLISH_QWERTY
                || value == KEYBOARD_TYPE_ENGLISH_NUMBER_SYMBOLS;
    }

    /**
     * 自绘键盘"按触摸点取按键"方法名的查找顺序：
     * <ul>
     *   <li>3.5.3 起为 x1(MotionEvent, boolean)（混淆改名）</li>
     *   <li>更早版本为 v1(MotionEvent, boolean)</li>
     * </ul>
     */
    static List<String> touchKeyButtonMethodCandidates() {
        return Arrays.asList("x1", "v1");
    }

    /** 自绘键盘"按坐标取按键"的兜底方法名：n.B1(int, int, boolean, boolean)。 */
    static String coordinateKeyButtonMethod() {
        return "B1";
    }
}
