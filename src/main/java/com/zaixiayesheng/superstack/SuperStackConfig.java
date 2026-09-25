package com.zaixiayesheng.superstack;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 配置。混入（Mixin）在游戏运行中被高频调用，所以这里把值缓存在静态字段里，
 * 配置加载/重载时刷新，避免每次都去读配置对象。
 */
public final class SuperStackConfig {
    /** 配置还没加载时的兜底值，跟默认值保持一致。 */
    public static final int DEFAULT_MAX_STACK_SIZE = 1000;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue MAX_STACK_SIZE = BUILDER
            .comment("物品的堆叠上限。原版是 64，默认 1000。改完需要重启游戏。",
                    "The new stack size limit for items. Vanilla is 64, default 1000. Restart required after changing.")
            .defineInRange("maxStackSize", DEFAULT_MAX_STACK_SIZE, 64, 1_000_000);

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

    static final ModConfigSpec SPEC = BUILDER.build();

    private static int maxStackSize = DEFAULT_MAX_STACK_SIZE;
    private static boolean scaleSmallStacks = true;
    private static int minimumStackSizeThreshold = 1;
    private static CountDisplay countDisplay = CountDisplay.COMPACT;
    private static int abbreviateFrom = 10000;
    private static boolean tooltipExactCount = true;
    private static boolean raiseModdedSlotLimits = true;

    private SuperStackConfig() {
    }

    public static int maxStackSize() {
        return maxStackSize;
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
        try {
            maxStackSize = MAX_STACK_SIZE.get();
            scaleSmallStacks = SCALE_SMALL_STACKS.get();
            minimumStackSizeThreshold = MIN_THRESHOLD.get();
            countDisplay = COUNT_DISPLAY.get();
            abbreviateFrom = ABBREVIATE_FROM.get();
            tooltipExactCount = TOOLTIP_COUNT.get();
            raiseModdedSlotLimits = RAISE_MODDED_SLOT_LIMITS.get();
            if (!raiseModdedSlotLimits) {
                SuperStack.LOGGER.warn("raiseModdedSlotLimits 已关闭：整理类模组（精妙整理等）整理大堆叠时可能会丢东西，"
                        + "只有遇到机器拒收物品才建议关掉这项。");
            }
        } catch (IllegalStateException e) {
            // 配置还没加载完就被调用，用默认值
            SuperStack.LOGGER.warn("配置尚未就绪，先使用默认值", e);
        }
    }

    /** 槽位里数量的显示方式。 */
    public enum CountDisplay {
        /** 超过门槛后用单位缩写（12.3k、1.23m），和精妙背包一致。 */
        COMPACT,
        /** 一律显示精确数字（缩放到塞得下为止）。 */
        EXACT
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
}
