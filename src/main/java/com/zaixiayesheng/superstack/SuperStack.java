package com.zaixiayesheng.superstack;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * 超级堆叠 / Super Stack
 *
 * 把物品的堆叠上限提高到可配置的值（默认 1000），并且让容器（背包、箱子、合成格……）
 * 真的能放下整叠，数量显示用精确数字而不是缩写。
 */
@Mod(SuperStack.MOD_ID)
public class SuperStack {
    public static final String MOD_ID = "superstack";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SuperStack(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, SuperStackConfig.SPEC);
        modEventBus.addListener(SuperStackConfig::onConfigLoad);
        modEventBus.addListener(ModBusEvents::onModifyDefaultComponents);
    }
}
