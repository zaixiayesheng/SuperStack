package com.zaixiayesheng.superstack;

import java.util.Locale;

/**
 * 服务端配置文件注释的语言选择。
 *
 * 注释是在构建配置时一次性写进 TOML 的，没有按语言分别存一份的机制，
 * 所以这里按运行环境的语言挑一份文案：中文或英文，其它语言回落到默认（中文）。
 * 可以用 -Dsuperstack.config.lang=zh 或 en 强制指定。
 */
public final class Text {
    private static final String PROPERTY = "superstack.config.lang";

    private static final boolean CHINESE = detectChinese();

    private Text() {
    }

    private static boolean detectChinese() {
        String forced = System.getProperty(PROPERTY, "").toLowerCase(Locale.ROOT);
        if (forced.startsWith("zh")) {
            return true;
        }
        if (forced.startsWith("en")) {
            return false;
        }
        String language = Locale.getDefault().getLanguage();
        if (language == null || language.isEmpty()) {
            return true;
        }
        if (language.startsWith("zh")) {
            return true;
        }
        if (language.startsWith("en")) {
            return false;
        }
        return true;
    }

    /** 当前是否使用中文注释。 */
    public static boolean chinese() {
        return CHINESE;
    }

    /** 按当前语言挑一组注释行。 */
    public static String[] pick(String[] chineseLines, String[] englishLines) {
        String[] lines = CHINESE ? chineseLines : englishLines;
        String[] result = new String[lines.length];
        for (int i = 0; i < lines.length; i++) {
            result[i] = " " + lines[i];
        }
        return result;
    }
}
