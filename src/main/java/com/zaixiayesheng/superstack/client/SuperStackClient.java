package com.zaixiayesheng.superstack.client;

import com.zaixiayesheng.superstack.SuperStack;
import com.zaixiayesheng.superstack.SuperStackLog;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** 客户端入口：注册客户端事件。 */
@Mod(value = SuperStack.MOD_ID, dist = Dist.CLIENT)
public class SuperStackClient {
    public SuperStackClient(ModContainer container) {
        // 提示框显示精确数量
        IEventBus gameBus = NeoForge.EVENT_BUS;
        gameBus.register(ClientEvents.class);

        SuperStackLog.info("客户端表现已就绪：槽位数量缩写 / 缩放显示、悬停显示精确数量。");
    }
}
