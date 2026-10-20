package com.zaixiayesheng.superstack;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * 配置。混入（Mixin）在游戏运行中被高频调用，所以这里把值缓存在静态字段里，
 * 配置加载/重载时刷新，避免每次都去读配置对象。
 *
 * <p>缓存字段是 {@code volatile}：热重载发生在别的线程，而读它的有渲染线程，
 * 不加的话可能出现「刚改完配置、游戏里还是旧值」这种要重启才好的假象。
 */
public final class SuperStackConfig {
    /** 配置还没加载时的兜底值，跟默认值保持一致。 */
    public static final int DEFAULT_MAX_STACK_SIZE = 32768;
    /** 联动模组看到的槽位上限：默认不额外限制，跟随堆叠上限；遇到第三方溢出时再单独调低。 */
    public static final int DEFAULT_COMPAT_STACK_LIMIT = Integer.MAX_VALUE;

    /** 原版的默认堆叠上限，缩放比例以它为基准。 */
    public static final int VANILLA_MAX_STACK_SIZE = 64;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue MAX_STACK_SIZE = BUILDER
            .comment("物品的堆叠上限。原版是 64，默认 32768，上限 2147483647。改完需要重启游戏。",
                    "The new stack size limit for items. Vanilla is 64, default 32768, upper bound Integer.MAX_VALUE (2147483647). Restart required after changing.")
            .defineInRange("maxStackSize", DEFAULT_MAX_STACK_SIZE, 64, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue COMPAT_STACK_LIMIT = BUILDER
            .comment("联动模组（通用机械、应用能源、精妙、更多熔炉、神秘农业）看到的槽位上限。默认跟随堆叠上限；只有当某个模组把槽位上限当数组长度或拿去做乘法、导致异常时，才需要把它调低。",
                    "Slot limit exposed to supported mods. Deliberately lower than the stack size; raise it only if you know those mods handle huge values.")
            .defineInRange("compatStackLimit", DEFAULT_COMPAT_STACK_LIMIT, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.BooleanValue SCALE_SMALL_STACKS = BUILDER
            .comment("小堆叠物品（比如末影珍珠 16 个）按比例放大：原上限 × maxStackSize ÷ 64。",
                    "关掉的话所有物品统一用 maxStackSize。",
                    "Whether items with smaller vanilla stack sizes (eg. ender pearls) are scaled proportionally.")
            .define("scaleSmallStacks", true);

    private static final ModConfigSpec.IntValue MIN_THRESHOLD = BUILDER
            .comment("原版上限不超过这个值的物品不放大。默认 1，也就是只有原本只能堆 1 个的物品（工具、桶之类）保持不变。")
            .defineInRange("minimumStackSizeThreshold", 1, 0, 64);

    private static final ModConfigSpec.EnumValue<CountDisplay> COUNT_DISPLAY = BUILDER
            .comment("槽位里数量怎么显示：",
                    "COMPACT —— 超过门槛后换成单位缩写（12.3k、1.23m），和精妙背包一致，默认；",
                    "EXACT —— 一律显示精确数字（缩放到塞得下为止）。",
                    "两种方式都能在提示框里看到精确数量。")
            .defineEnum("countDisplay", CountDisplay.COMPACT);

    private static final ModConfigSpec.IntValue ABBREVIATE_FROM = BUILDER
            .comment("数量达到多少开始用单位缩写。默认 10000（也就是 1000~9999 还是显示 1,234 这种精确写法，和精妙背包一致）。",
                    "想让它从 1000 就开始缩写就填 1000。")
            .defineInRange("abbreviateFrom", 10000, 1000, 1_000_000);

    private static final ModConfigSpec.BooleanValue TOOLTIP_COUNT = BUILDER
            .comment("鼠标悬停时在提示框里显示精确数量（数量超过 99 才显示）。")
            .define("tooltipExactCount", true);

    private static final ModConfigSpec.BooleanValue RAISE_MODDED_SLOT_LIMITS = BUILDER
            .comment("是否也放大其它模组的物品槽上限（ItemStackHandler / ComponentItemHandler）。",
                    "默认打开：有些整理模组（比如精妙整理）整理时会先把槽位内容复制进一个这样的物品槽再排回去，",
                    "如果这个槽位上限还停在自己的默认值（NeoForge 的是 99），大堆叠就会在整理时被截断丢掉。",
                    "实测：10000 的堆叠 + 精妙整理整理背包，关掉这项会从 270000 掉到 2673，打开则一个不少。",
                    "如果有机器因此拒收物品，再把它关掉。")
            .define("raiseModdedSlotLimits", true);


    private static final ModConfigSpec.IntValue FAN_PROCESSING_VIRTUAL_STACK_SIZE = BUILDER
            .comment("鼓风机的处理时长按「这一堆算多少个」来计算，原版公式是 (数量-1)/16+1。这里给参与计算的数量加一个上限：堆叠不超过它时用真实数量（与原版一致），超过它时按它计算。64 等于原版一组的速度，调小更快，1 最快。",
                    "The fan processing time is computed from a stack count with the vanilla formula (count-1)/16+1. This value caps the count used: stacks at or below it use their real count (vanilla behaviour), larger stacks are treated as this value. 64 equals the vanilla 64-stack speed, lower is faster, 1 is the fastest.")
            .defineInRange("fanProcessingVirtualStackSize", 64, 1, 64);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static volatile int maxStackSize = DEFAULT_MAX_STACK_SIZE;
    private static volatile int compatStackLimit = DEFAULT_COMPAT_STACK_LIMIT;
    private static volatile boolean scaleSmallStacks = true;
    private static volatile int minimumStackSizeThreshold = 1;
    private static volatile CountDisplay countDisplay = CountDisplay.COMPACT;
    private static volatile int abbreviateFrom = 10000;
    private static volatile boolean tooltipExactCount = true;
    private static volatile boolean raiseModdedSlotLimits = true;
    private static volatile int fanProcessingVirtualStackSize = 64;

    private SuperStackConfig() {
    }

    public static int maxStackSize() {
        return maxStackSize;
    }

    public static int compatStackLimit() {
        return compatStackLimit;
    }

    public static int fanProcessingVirtualStackSize() {
        return fanProcessingVirtualStackSize;
    }

    public static boolean scaleSmallStacks() {
        return scaleSmallStacks;
    }

    public static int minimumStackSizeThreshold() {
        return minimumStackSizeThreshold;
    }

    public static CountDisplay countDisplay() {
        return countDisplay;
    }

    public static int abbreviateFrom() {
        return abbreviateFrom;
    }

    public static boolean tooltipExactCount() {
        return tooltipExactCount;
    }

    public static boolean raiseModdedSlotLimits() {
        return raiseModdedSlotLimits;
    }


    // ==== 下面几个方法是给混入调用的，只在"原版默认值"上动手，其余保持原样，尽量不影响别的模组 ====

    public static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        // 热重载和首次加载要分开报：首次报总账，重载只报「变了什么」
        boolean firstLoad = event instanceof ModConfigEvent.Loading;
        int previousMax = maxStackSize;
        boolean previousScale = scaleSmallStacks;
        CountDisplay previousDisplay = countDisplay;
        int previousAbbreviate = abbreviateFrom;

        try {
            maxStackSize = MAX_STACK_SIZE.get();
        compatStackLimit = COMPAT_STACK_LIMIT.get();
            scaleSmallStacks = SCALE_SMALL_STACKS.get();
            minimumStackSizeThreshold = MIN_THRESHOLD.get();
            countDisplay = COUNT_DISPLAY.get();
            abbreviateFrom = ABBREVIATE_FROM.get();
            tooltipExactCount = TOOLTIP_COUNT.get();
            raiseModdedSlotLimits = RAISE_MODDED_SLOT_LIMITS.get();
            fanProcessingVirtualStackSize = FAN_PROCESSING_VIRTUAL_STACK_SIZE.get();
        } catch (IllegalStateException e) {
            // 配置还没加载完就被调用，用默认值
            SuperStackLog.warn("配置尚未就绪，先使用默认值", e);
            return;
        }

        if (firstLoad) {
            SuperStackLog.rememberConfigPath(String.valueOf(event.getConfig().getFullPath()));
            SuperStackLog.logStartup(logSummary());
        } else {
            logReload(previousMax, previousScale, previousDisplay, previousAbbreviate);
        }

        logWarnings();
    }

    /** 启动时那行配置摘要，一眼能看出上限到底生效成多少。 */
    private static String logSummary() {
        StringBuilder sb = new StringBuilder("配置：堆叠上限 ");
        sb.append(formatLimit(maxStackSize));
        sb.append("（原版 64）");

        sb.append("，小堆叠按比例放大：");
        if (scaleSmallStacks) {
            sb.append("开（").append(minimumStackSizeThreshold).append(" 以上的按 原上限×")
                    .append(maxStackSize).append("÷64 算）");
        } else {
            sb.append("关（所有物品统一上限）");
        }

        sb.append("，数量显示：");
        if (countDisplay == CountDisplay.EXACT) {
            sb.append("精确数字");
        } else {
            sb.append("缩写（满 ").append(formatLimit(abbreviateFrom)).append(" 起用 k/m）");
        }

        sb.append("，其它模组的物品槽上限：").append(raiseModdedSlotLimits ? "跟随" : "不动");
        return sb.toString();
    }

    /** 热重载时只报变化，没变就一行都不刷。 */
    private static void logReload(int previousMax, boolean previousScale, CountDisplay previousDisplay,
                                  int previousAbbreviate) {
        List<String> changes = new ArrayList<>();
        if (previousMax != maxStackSize) {
            changes.add("堆叠上限 " + formatLimit(previousMax) + " → " + formatLimit(maxStackSize));
        }
        if (previousScale != scaleSmallStacks) {
            changes.add("小堆叠按比例放大 " + (previousScale ? "开 → 关" : "关 → 开"));
        }
        if (previousDisplay != countDisplay) {
            changes.add("数量显示 " + previousDisplay + " → " + countDisplay);
        }
        if (previousAbbreviate != abbreviateFrom) {
            changes.add("缩写门槛 " + formatLimit(previousAbbreviate) + " → " + formatLimit(abbreviateFrom));
        }

        if (changes.isEmpty()) {
            return;
        }
        SuperStackLog.info("配置已重载：" + String.join("；", changes));
        if (previousMax != maxStackSize) {
            SuperStackLog.warn("堆叠上限的改变对已加载的存档不会追溯生效，建议重启游戏。");
        }
    }

    /** 配置本身没毛病、但玩法上会踩坑的地方，只在启动时报一次。 */
    private static void logWarnings() {
        if (scaleSmallStacks && maxStackSize < VANILLA_MAX_STACK_SIZE) {
            SuperStackLog.warn("maxStackSize = " + maxStackSize + " 比原版的 " + VANILLA_MAX_STACK_SIZE
                    + " 还小：原本堆 64 的物品不变，末影珍珠这类反而会被压到 " + maxStackSize + "。");
        }

        if (!raiseModdedSlotLimits) {
            SuperStackLog.warn("raiseModdedSlotLimits 已关闭：整理类模组（精妙整理等）整理大堆叠时会截断丢东西，"
                    + "只有遇到机器拒收物品才建议关掉这项。");
        }
    }

    /** 原版 Container/Slot 的默认上限就是 99，只把这个值换成我们的上限。 */
    public static int raiseVanillaDefault(int original) {
        return original == 99 ? maxStackSize : original;
    }

    /** 原版 ExtraCodecs.intRange(1, 99) 是堆叠上限的校验范围，把上界换掉。 */
    public static int raiseCodecUpperBound(int bound) {
        return bound == 99 ? maxStackSize : bound;
    }

    /** 其它模组的物品槽上限（可选，默认不动，只升不降）。 */
    public static int raiseModdedSlotLimit(int original) {
        return raiseModdedSlotLimits ? Math.max(original, maxStackSize) : original;
    }

    /**
     * 模组自己写死的老上限：堆叠上限 64、容器默认值 99。
     *
     * <p>通用机械的槽位、应用能源的内部库存、精妙的库存分区都在各自的类里把上限
     * 固定成这两个值，光改物品组件够不着它们。这里只替换「正好等于 64 或 99」的返回值，
     * 1、16、1024 这类明确的小上限保持原样，免得出意外。
     */
    public static int raiseLegacyDefault(int original) {
        if (!raiseModdedSlotLimits) {
            return original;
        }
        return original == VANILLA_MAX_STACK_SIZE || original == 99 ? maxStackSize : original;
    }

    /** 联动模组用的版本：把写死的 64 / 99 换成 compatStackLimit。 */
    public static int raiseCompatLimit(int original) {
        if (!raiseModdedSlotLimits) {
            return original;
        }
        return original == VANILLA_MAX_STACK_SIZE || original == 99 ? Math.min(compatStackLimit, maxStackSize) : original;
    }

    // ==== 下面两个是启动日志用的，ModifyDefaultComponentsEvent 和日志共用同一套缩放规则 ====

    /**
     * 小堆叠物品的新上限：原上限 × maxStackSize ÷ 64，封顶 maxStackSize。
     *
     * <p>{@code scaleSmallStacks} 关掉时统一返回 maxStackSize。
     */
    public static int scaledMax(int vanillaMax) {
        if (!scaleSmallStacks) {
            return maxStackSize;
        }
        return (int) Math.clamp((long) vanillaMax * maxStackSize / VANILLA_MAX_STACK_SIZE, 1L, (long) maxStackSize);
    }

    /**
     * 把「原上限 → 新上限」的对应关系缩成一行，只报用户看得懂的几档（64 / 16 / 4 …）。
     * 顺带把缩放的边界讲清楚，省得有人以为末影珍珠也会变成 64 的倍数。
     *
     * <p>这里用精确数字而不是缩写：缩放比例是这张表存在的唯一理由，
     * 写成 `16 → 8.19k` 就看不出它到底是不是整数倍了。
     */
    public static String summarizeDefaults(int[] vanillaSizes) {
        Set<ScaleEntry> entries = new LinkedHashSet<>();
        for (int vanilla : vanillaSizes) {
            entries.add(new ScaleEntry(vanilla, scaledMax(vanilla)));
        }

        List<ScaleEntry> sorted = new ArrayList<>(new TreeSet<>(entries));
        StringBuilder sb = new StringBuilder();
        for (ScaleEntry entry : sorted) {
            if (!sb.isEmpty()) {
                sb.append("，");
            }
            sb.append(CountFormat.exact(entry.vanilla)).append(" → ").append(CountFormat.exact(entry.newMax));
        }
        return sb.toString();
    }

    /** 数量缩写，和槽位里显示的是同一套规则。 */
    public static String formatLimit(int value) {
        return CountFormat.compact(value);
    }

    /** 一组「原上限 → 新上限」，用来在日志里去重和排序。 */
    private record ScaleEntry(int vanilla, int newMax) implements Comparable<ScaleEntry> {
        @Override
        public int compareTo(ScaleEntry other) {
            int byNew = Integer.compare(other.newMax, newMax);
            return byNew != 0 ? byNew : Integer.compare(other.vanilla, vanilla);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ScaleEntry other && vanilla == other.vanilla && newMax == other.newMax;
        }

        @Override
        public int hashCode() {
            return Objects.hash(vanilla, newMax);
        }
    }

    /** 槽位里数量的显示方式。 */
    public enum CountDisplay {
        /** 超过门槛后用单位缩写（12.3k、1.23m），和精妙背包一致。 */
        COMPACT,
        /** 一律显示精确数字（缩放到塞得下为止）。 */
        EXACT
    }
}
