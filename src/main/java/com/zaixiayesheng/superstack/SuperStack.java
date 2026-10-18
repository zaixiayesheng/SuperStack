package com.zaixiayesheng.superstack;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

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

    public SuperStack() {
        // 记下构造时刻，配置加载完报「多久之后上限开始生效」
        SuperStackLog.markConstructed();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SuperStackConfig.SPEC);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(SuperStackConfig::onConfigLoad);
        modBus.addListener(SuperStackLog::onCommonSetup);
    }
}
