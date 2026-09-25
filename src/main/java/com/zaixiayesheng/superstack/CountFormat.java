package com.zaixiayesheng.superstack;

import java.util.Locale;

/**
 * 数量文字的格式化，规则对着精妙背包（Sophisticated Backpacks）来：
 *
 * <ul>
 *   <li>没到缩写门槛时用千位分隔符：{@code 1,234}；</li>
 *   <li>超过门槛后换成小写单位后缀，保留约 4 位有效数字：{@code 12.3k}、{@code 1.23m}、{@code 999m}；</li>
 *   <li>一律**截断**而不是四舍五入（精妙也是截断），免得显示的数字比实际多。</li>
 * </ul>
 */
public final class CountFormat {
    private static final String[] SUFFIXES = {"k", "m", "b"};

    private CountFormat() {
    }

    /** 槽位里显示的紧凑写法。 */
    public static String compact(int count) {
        if (count < SuperStackConfig.abbreviateFrom()) {
            return exact(count);
        }

        double value = count;
        int unit = 0;
        while (unit < SUFFIXES.length && value >= 1000.0D) {
            value /= 1000.0D;
            unit++;
        }
        if (unit == 0) {
            return exact(count);
        }

        // 目标约 4 位有效数字
        int decimals = value < 10.0D ? 2 : (value < 100.0D ? 1 : 0);
        double factor = Math.pow(10.0D, decimals);
        value = Math.floor(value * factor) / factor;

        String number = decimals == 0
                ? String.valueOf((long) value)
                : String.format(Locale.ROOT, "%." + decimals + "f", value);
        return number + SUFFIXES[unit - 1];
    }

    /** 精确写法，带千位分隔符。 */
    public static String exact(int count) {
        return String.format(Locale.ROOT, "%,d", count);
    }
}
