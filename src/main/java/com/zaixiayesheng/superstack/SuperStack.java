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
