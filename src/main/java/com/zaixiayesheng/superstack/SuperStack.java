package com.zaixiayesheng.superstack;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * 超级堆叠 / Super Stack
 *
 * <p>把物品的堆叠上限提高到可配置的值（默认 32768），并且让容器（背包、箱子、合成格……）
 * 真的能放下整叠；数量显示用精确数字或缩写，悬停时给出精确值。
 *
 * <p>具体改了哪几处、有哪些已知坑，见 README。
 */
@Mod(SuperStack.MOD_ID)
public class SuperStack {
    public static final String MOD_ID = "superstack";

    /**
     * 存档/网络格式能接受的最大堆叠数量，与配置无关。
     *
     * <p>{@code ItemStack.CODEC} 在类初始化时就构建好，那时配置文件还没读，
     * 所以编码范围不能跟着配置走，必须是一个编译期常量；配置只决定"默认给多少"。
     * 取 {@link Integer#MAX_VALUE} 的一半而不是原值，是为了让 {@code a + b} 这类 int 加法不会溢出；
     * StackSizeTweaks 用 10 亿、BiggerStacks 用 1,073,741,823（同一个值），都是这个思路。
     */
    public static final int ABSOLUTE_MAX_STACK_SIZE = Integer.MAX_VALUE / 2;

    public SuperStack(IEventBus modEventBus, ModContainer modContainer) {
        // 记下构造时刻，配置加载完报「多久之后上限开始生效」
        SuperStackLog.markConstructed();

        if (System.getProperty("superstack.debug.forcecompat") != null) {
            SuperStackLog.probeCompatTargets();
        }

        modContainer.registerConfig(ModConfig.Type.COMMON, SuperStackConfig.SPEC);
        modEventBus.addListener(SuperStackConfig::onConfigLoad);
        modEventBus.addListener(ModBusEvents::onModifyDefaultComponents);
    }
}
