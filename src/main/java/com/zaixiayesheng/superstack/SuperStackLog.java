package com.zaixiayesheng.superstack;

import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志。
 *
 * <p>这个模组改的是全局规则（堆叠上限、容器上限、数量显示），出问题的时候光看游戏里
 * 的现象根本判断不出是「没加载」「配置没生效」还是「和别的模组打架」，所以日志按
 * 「启动时报清楚改了什么、改了多少、有哪些坑」来写，全部带 {@value #PREFIX} 前缀，
 * 玩家在 latest.log 里搜这一个词就能看全。
 *
 * <p>排查堆叠问题的人多半会顺手里要日志，所以这里刻意不写 DEBUG —— 默认等级就能看全，
 * 而且是有限几行，不刷屏。
 */
public final class SuperStackLog {
    /** 日志前缀，方便在游戏日志里过滤。 */
    public static final String PREFIX = "[SuperStack]";

    private static final Logger LOGGER = LoggerFactory.getLogger("SuperStack");

    /**
     * 已知会和我们动同一批东西的模组（modId → 装了什么、要注意什么）。
     * 只提示，不拦启动，也不做任何 incompatibility 声明。
     */
    private static final String[][] CONFLICT_HINTS = {
            {"sophisticatedbackpacks", "精妙背包：数量缩写规则和它对齐，大堆叠在同一容器里混用时留意显示"},
            {"inventorysorter", "一键背包整理：读取上限走 ItemStack.getMaxStackSize()，放不下会留在原地，不会丢东西"},
            {"invtweaks", "一键背包整理Next：同上，整理前会在本地沙盒里先模拟一遍"},
            {"sophisticatedcore", "精妙核心（整理）：整理时会把槽位内容复制进 ItemStackHandler，"
                    + "otherModSlots 关掉会截断大堆叠"},
            {"kubejs", "KubeJS：它的 remove_slot_limit / default_max_stack_size 默认透传，不会把上限压回去"},
    };

    private static String version = "unknown";
    private static String configPath = "config/superstack-common.toml";
    private static long constructedAtNanos;

    private SuperStackLog() {
    }

    /** 模组构造时记一笔，用来算「从开始加载到配置就绪」用了多久。 */
    static void markConstructed() {
        version = resolveVersion();
        constructedAtNanos = System.nanoTime();
    }

    /**
     * 配置首次加载完 = 上限真的开始生效了，在这里报一次总账。
     * 之后的热重载不再重复（那时只报配置差异，见 {@link SuperStackConfig}）。
     */
    static void logStartup(String configSummary) {
        long elapsedMs = constructedAtNanos == 0L ? -1L : (System.nanoTime() - constructedAtNanos) / 1_000_000L;

        LOGGER.info("{}超级堆叠 v{} 已加载（NeoForge 1.21.1）", PREFIX, version);
        LOGGER.info("{}{}", PREFIX, configSummary);
        LOGGER.info("{}配置文件：{}{}", PREFIX, configPath,
                elapsedMs >= 0L ? "（上限在启动 " + elapsedMs + "ms 后生效）" : "");

        logLoadedHints();
    }

    /** 启动时列出「装了哪些需要留意的模组」，一行一个，没有就一行说明。 */
    private static void logLoadedHints() {
        List<String> present = new ArrayList<>();
        for (String[] hint : CONFLICT_HINTS) {
            if (ModList.get().isLoaded(hint[0])) {
                present.add(hint[1]);
            }
        }

        if (present.isEmpty()) {
            LOGGER.info("{}没有检测到需要额外留意的模组。", PREFIX);
            return;
        }
        LOGGER.info("{}检测到 {} 个相关模组，留意：", PREFIX, present.size());
        for (String line : present) {
            info("  - " + line);
        }
    }

    /** 首次加载记下配置文件的真实路径，启动日志里报给玩家，省得他去找。 */
    static void rememberConfigPath(String path) {
        if (path != null && !path.isBlank()) {
            configPath = path;
        }
    }

    // 下面几个留给 mod 自己的其它包调用（mixin、client），所以是 public
    public static void info(String message) {
        LOGGER.info("{}{}", PREFIX, message);
    }

    /**
     * 只有排查具体渲染 / 数值问题时才需要看的东西。默认等级下不打，
     * 调用方要先问 {@link #isDebugEnabled()}，免得白拼字符串（这行代码每帧都在跑）。
     */
    public static void debug(String message) {
        LOGGER.debug("{}{}", PREFIX, message);
    }

    public static boolean isDebugEnabled() {
        return LOGGER.isDebugEnabled();
    }

    /**
     * 排查兼容补丁时用：提前把目标类加载出来，让 Mixin 当场注入，注入失败会直接报在日志里。
     * 只有带 {@code -Dsuperstack.debug.forcecompat} 启动时才跑。
     */
    public static void probeCompatTargets() {
        String[] targets = {
                "net.p3pp3rf1y.sophisticatedcore.util.InventorySorter",
                "net.p3pp3rf1y.sophisticatedcore.inventory.StatefulComponentItemHandler",
                "com.blakebr0.cucumber.inventory.BaseItemStackHandler",
                "com.jerry.meklm.common.inventory.slot.BigStackInputInventorySlot",
                "com.jerry.meklm.common.inventory.slot.BigStackOutputInventorySlot",
                "appeng.util.inv.AppEngInternalInventory",
                          "appeng.menu.slot.AppEngSlot",
                "appeng.util.inv.PlayerInternalInventory",
                "appeng.util.inv.CarriedItemInventory",
                "mekanism.common.inventory.slot.BasicInventorySlot",
                "ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase",
                "com.simibubi.create.content.kinetics.fan.processing.FanProcessing"
        };
        for (String target : targets) {
            try {
                Class.forName(target);
                info("兼容目标已加载：" + target);
            } catch (Throwable t) {
                debug("兼容目标不存在：" + target);
            }
        }
    }

    /** 出异常时用，带上堆栈。 */
    public static void warn(String message, Throwable cause) {
        LOGGER.warn("{}{}", PREFIX, message, cause);
    }

    public static void warn(String message) {
        LOGGER.warn("{}{}", PREFIX, message);
    }

    /**
     * 从 ModList 里取自己的版本号，取不到就退回 "unknown"。
     * 只在构造时调一次 —— 那时候日志系统已经起来了，但配置还没加载。
     */
    private static String resolveVersion() {
        try {
            return ModList.get().getModContainerById(SuperStack.MOD_ID)
                    .map(container -> container.getModInfo().getVersion().toString())
                    .orElse("unknown");
        } catch (Throwable t) {
            // 版本号只是日志里的装饰，取不到不能影响加载
            return "unknown";
        }
    }
}
