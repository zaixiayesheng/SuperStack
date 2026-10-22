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

    /** 原版的默认堆叠上限，缩放比例以它为基准。 */
    public static final int VANILLA_MAX_STACK_SIZE = 64;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("basic");
    }

    private static final ModConfigSpec.IntValue MAX_STACK_SIZE = BUILDER
            .comment(Text.pick(new String[]{"一个格子能堆多少。原版是 64。改完重启游戏。"}, new String[]{"How many items fit in one slot. Vanilla is 64. Restart after changing."}))
            .translation("superstack.configuration.maxStackSize")
            .defineInRange("maxStackSize", DEFAULT_MAX_STACK_SIZE, 64, Integer.MAX_VALUE);

    private static final ModConfigSpec.BooleanValue SCALE_SMALL_ITEMS = BUILDER
            .comment(Text.pick(new String[]{"末影珍珠、鸡蛋这种不到 64 的物品也按原比例放大。"}, new String[]{"Items that stack below 64 (ender pearls, eggs) scale up by the same ratio."}))
            .translation("superstack.configuration.scaleSmallItems")
            .define("scaleSmallItems", true);

    private static final ModConfigSpec.BooleanValue KEEP_UNSTACKABLE = BUILDER
            .comment(Text.pick(new String[]{"工具、桶、护甲这类只能放 1 个的保持 1 个。"}, new String[]{"Keep tools, buckets and armour at one per slot."}))
            .translation("superstack.configuration.keepUnstackable")
            .define("keepUnstackable", true);

    static {
        BUILDER.pop();
        BUILDER.push("advanced");
    }

    private static final ModConfigSpec.EnumValue<CountDisplay> COUNT_STYLE = BUILDER
            .comment(Text.pick(new String[]{"槽位里的数字怎么画：", "COMPACT —— 到门槛后写成 12.3k、1.23m；", "EXACT —— 一直写完整数字。"}, new String[]{"How slot counts are drawn:", "COMPACT: 12.3k / 1.23m past the threshold;", "EXACT: always the full number."}))
            .translation("superstack.configuration.countStyle")
            .defineEnum("countStyle", CountDisplay.COMPACT);

    private static final ModConfigSpec.IntValue ABBREVIATE_ABOVE = BUILDER
            .comment(Text.pick(new String[]{"从多少个开始缩写。默认 10000，也就是 1,234 这种照样写全。"}, new String[]{"Count from which abbreviations start. Default 10000 keeps 1,234 style below that."}))
            .translation("superstack.configuration.abbreviateAbove")
            .defineInRange("abbreviateAbove", 10000, 1000, 1_000_000);

    private static final ModConfigSpec.BooleanValue TOOLTIP_EXACT = BUILDER
            .comment(Text.pick(new String[]{"鼠标悬停时在提示框显示精确数量。"}, new String[]{"Show the exact count on the tooltip."}))
            .translation("superstack.configuration.tooltipExact")
            .define("tooltipExact", true);

    private static final ModConfigSpec.BooleanValue OTHER_MOD_SLOTS = BUILDER
            .comment(Text.pick(new String[]{"顺带放宽其它模组的槽位上限，防止整理背包时截断大堆叠。", "遇到机器拒收物品再关掉。"}, new String[]{"Also raise other mods' slot limits so sorting does not truncate big stacks.", "Turn it off if some machine refuses items."}))
            .translation("superstack.configuration.otherModSlots")
            .define("otherModSlots", true);

    private static final ModConfigSpec.IntValue FAN_STACK_COUNT = BUILDER
            .comment(Text.pick(new String[]{"机械动力鼓风机按多少个来算时长。64 等于原版一组的速度。"}, new String[]{"Stack count used for Create fan processing time. 64 equals the vanilla 64-stack speed."}))
            .translation("superstack.configuration.fanStackCount")
            .defineInRange("fanStackCount", 64, 1, 64);

    static {
        BUILDER.pop();
    }


    static {
        BUILDER.push("compat");
    }

    private static final ModConfigSpec.BooleanValue MEKANISM_PATCH = BUILDER
            .comment(Text.pick(new String[]{"通用机械：让机器槽位吃下超大堆叠。"}, new String[]{"Mekanism machine slots accept larger stacks."}))
            .translation("superstack.configuration.compatMekanism")
            .define("compatMekanism", true);

    private static final ModConfigSpec.BooleanValue MEKMM_PATCH = BUILDER
            .comment(Text.pick(new String[]{"更多机器：大型机器的堆叠槽位。"}, new String[]{"Mekanism: MoreMachine big-stack slots."}))
            .translation("superstack.configuration.compatMekmm")
            .define("compatMekmm", true);

    private static final ModConfigSpec.BooleanValue AE2_PATCH = BUILDER
            .comment(Text.pick(new String[]{"应用能源 2：终端与合成格。"}, new String[]{"Applied Energistics 2 terminals and slots."}))
            .translation("superstack.configuration.compatAe2")
            .define("compatAe2", true);

    private static final ModConfigSpec.BooleanValue SOPHISTICATED_PATCH = BUILDER
            .comment(Text.pick(new String[]{"精妙：容器与合成升级的合成格。"}, new String[]{"Sophisticated containers and crafting upgrade grid."}))
            .translation("superstack.configuration.compatSophisticated")
            .define("compatSophisticated", true);

    private static final ModConfigSpec.BooleanValue IRON_FURNACES_PATCH = BUILDER
            .comment(Text.pick(new String[]{"更多熔炉：输出格上限。"}, new String[]{"Iron Furnaces output slot."}))
            .translation("superstack.configuration.compatIronFurnaces")
            .define("compatIronFurnaces", true);

    private static final ModConfigSpec.BooleanValue MYSTICAL_AGRICULTURE_PATCH = BUILDER
            .comment(Text.pick(new String[]{"神秘农业：机器内部槽位。"}, new String[]{"Mystical Agriculture machines (Cucumber item handler)."}))
            .translation("superstack.configuration.compatMysticalAgriculture")
            .define("compatMysticalAgriculture", true);

    private static final ModConfigSpec.BooleanValue CREATE_PATCH = BUILDER
            .comment(Text.pick(new String[]{"机械动力：鼓风机时长与过滤器设置界面。"}, new String[]{"Create fans and filter value screens."}))
            .translation("superstack.configuration.compatCreate")
            .define("compatCreate", true);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    private static volatile int maxStackSize = DEFAULT_MAX_STACK_SIZE;
    private static volatile boolean scaleSmallItems = true;
    private static volatile boolean keepUnstackable = true;
    private static volatile CountDisplay countStyle = CountDisplay.COMPACT;
    private static volatile int abbreviateAbove = 10000;
    private static volatile boolean tooltipExact = true;
    private static volatile boolean otherModSlots = true;
    private static volatile int fanStackCount = 64;
    private static volatile boolean compatMekanism = true;
    private static volatile boolean compatMekmm = true;
    private static volatile boolean compatAe2 = true;
    private static volatile boolean compatSophisticated = true;
    private static volatile boolean compatIronFurnaces = true;
    private static volatile boolean compatMysticalAgriculture = true;
    private static volatile boolean compatCreate = true;


    public enum CompatPatch {
        MEKANISM, MEKMM, AE2, SOPHISTICATED, IRON_FURNACES, MYSTICAL_AGRICULTURE, CREATE;
    }

    public static boolean compatEnabled(CompatPatch patch) {
        return switch (patch) {
            case MEKANISM -> compatMekanism;
            case MEKMM -> compatMekmm;
            case AE2 -> compatAe2;
            case SOPHISTICATED -> compatSophisticated;
            case IRON_FURNACES -> compatIronFurnaces;
            case MYSTICAL_AGRICULTURE -> compatMysticalAgriculture;
            case CREATE -> compatCreate;
        };
    }

    public static int raiseCompatLimit(CompatPatch patch, int original) {
        return compatEnabled(patch) ? raiseLegacyDefault(original) : original;
    }

    public static int ironFurnaceLimit(int original) {
        return compatEnabled(CompatPatch.IRON_FURNACES) ? maxStackSize : original;
    }

    public static int aeAmountPerOperation(int original) {
        return compatEnabled(CompatPatch.AE2) ? maxStackSize : original;
    }

    public static int createFilterLimit(int original) {
        return compatEnabled(CompatPatch.CREATE) ? Math.min(original, VANILLA_MAX_STACK_SIZE) : original;
    }

    public static int fanCountFor(int count) {
        if (!compatEnabled(CompatPatch.CREATE)) {
            return count;
        }
        int limit = fanStackCount;
        return count < limit ? count : limit;
    }

    public static int mekmmSlotLimit(int original) {
        if (!compatEnabled(CompatPatch.MEKMM)) {
            return original;
        }
        return original > maxStackSize ? maxStackSize : original;
    }
    private SuperStackConfig() {
    }

    public static int maxStackSize() {
        return maxStackSize;
    }

    public static boolean scaleSmallItems() {
        return scaleSmallItems;
    }

    public static boolean keepUnstackable() {
        return keepUnstackable;
    }

    public static CountDisplay countStyle() {
        return countStyle;
    }

    public static int abbreviateAbove() {
        return abbreviateAbove;
    }

    public static boolean tooltipExact() {
        return tooltipExact;
    }

    public static boolean otherModSlots() {
        return otherModSlots;
    }

    public static int fanStackCount() {
        return fanStackCount;
    }

    // ==== 下面几个方法是给混入调用的，只在"原版默认值"上动手，其余保持原样，尽量不影响别的模组 ====

    public static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }

        // 热重载和首次加载要分开报：首次报总账，重载只报「变了什么」
        boolean firstLoad = event instanceof ModConfigEvent.Loading;
        int previousMax = maxStackSize;
        boolean previousScale = scaleSmallItems;
        CountDisplay previousDisplay = countStyle;
        int previousAbbreviate = abbreviateAbove;

        try {
            maxStackSize = MAX_STACK_SIZE.get();
            scaleSmallItems = SCALE_SMALL_ITEMS.get();
            keepUnstackable = KEEP_UNSTACKABLE.get();
            countStyle = COUNT_STYLE.get();
            abbreviateAbove = ABBREVIATE_ABOVE.get();
            tooltipExact = TOOLTIP_EXACT.get();
            otherModSlots = OTHER_MOD_SLOTS.get();
            fanStackCount = FAN_STACK_COUNT.get();
            compatMekanism = MEKANISM_PATCH.get();
            compatMekmm = MEKMM_PATCH.get();
            compatAe2 = AE2_PATCH.get();
            compatSophisticated = SOPHISTICATED_PATCH.get();
            compatIronFurnaces = IRON_FURNACES_PATCH.get();
            compatMysticalAgriculture = MYSTICAL_AGRICULTURE_PATCH.get();
            compatCreate = CREATE_PATCH.get();

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
        if (scaleSmallItems) {
            sb.append("开（").append(keepUnstackable).append(" 以上的按 原上限×")
                    .append(maxStackSize).append("÷64 算）");
        } else {
            sb.append("关（所有物品统一上限）");
        }

        sb.append("，数量显示：");
        if (countStyle == CountDisplay.EXACT) {
            sb.append("精确数字");
        } else {
            sb.append("缩写（满 ").append(formatLimit(abbreviateAbove)).append(" 起用 k/m）");
        }

        sb.append("，其它模组的物品槽上限：").append(otherModSlots ? "跟随" : "不动");
        return sb.toString();
    }

    /** 热重载时只报变化，没变就一行都不刷。 */
    private static void logReload(int previousMax, boolean previousScale, CountDisplay previousDisplay,
                                  int previousAbbreviate) {
        List<String> changes = new ArrayList<>();
        if (previousMax != maxStackSize) {
            changes.add("堆叠上限 " + formatLimit(previousMax) + " → " + formatLimit(maxStackSize));
        }
        if (previousScale != scaleSmallItems) {
            changes.add("小堆叠按比例放大 " + (previousScale ? "开 → 关" : "关 → 开"));
        }
        if (previousDisplay != countStyle) {
            changes.add("数量显示 " + previousDisplay + " → " + countStyle);
        }
        if (previousAbbreviate != abbreviateAbove) {
            changes.add("缩写门槛 " + formatLimit(previousAbbreviate) + " → " + formatLimit(abbreviateAbove));
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
        if (scaleSmallItems && maxStackSize < VANILLA_MAX_STACK_SIZE) {
            SuperStackLog.warn("maxStackSize = " + maxStackSize + " 比原版的 " + VANILLA_MAX_STACK_SIZE
                    + " 还小：原本堆 64 的物品不变，末影珍珠这类反而会被压到 " + maxStackSize + "。");
        }

        if (!otherModSlots) {
            SuperStackLog.warn("otherModSlots 已关闭：整理类模组（精妙整理等）整理大堆叠时会截断丢东西，"
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
        return otherModSlots ? Math.max(original, maxStackSize) : original;
    }

    /**
     * 模组自己写死的老上限：堆叠上限 64、容器默认值 99。
     *
     * <p>通用机械的槽位、应用能源的内部库存、精妙的库存分区都在各自的类里把上限
     * 固定成这两个值，光改物品组件够不着它们。这里只替换「正好等于 64 或 99」的返回值，
     * 1、16、1024 这类明确的小上限保持原样，免得出意外。
     */
    public static int raiseLegacyDefault(int original) {
        if (!otherModSlots) {
            return original;
        }
        return original == VANILLA_MAX_STACK_SIZE || original == 99 ? maxStackSize : original;
    }

    /** 联动模组用的版本：把写死的 64 / 99 换成堆叠上限。 */
    public static int raiseCompatLimit(int original) {
        return raiseLegacyDefault(original);
    }

    // ==== 下面两个是启动日志用的，ModifyDefaultComponentsEvent 和日志共用同一套缩放规则 ====

    /**
     * 小堆叠物品的新上限：原上限 × maxStackSize ÷ 64，封顶 maxStackSize。
     *
     * <p>{@code scaleSmallItems} 关掉时统一返回 maxStackSize。
     */
    public static int scaledMax(int vanillaMax) {
        if (!scaleSmallItems) {
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
