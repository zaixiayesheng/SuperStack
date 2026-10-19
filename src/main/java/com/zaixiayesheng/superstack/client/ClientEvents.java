package com.zaixiayesheng.superstack.client;

import com.zaixiayesheng.superstack.CountFormat;
import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 客户端表现：提示框里给出「当前数量 / 上限」。
 *
 * <p>精妙自己的界面与提示框都会显示数量，所以那边不重复加。
 */
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!SuperStackConfig.tooltipExactCount() || superstack$isSophisticatedScreen()) {
            return;
        }
        int count = event.getItemStack().getCount();
        if (count > 99) {
            // 槽位里可能是缩写（12.3k），悬停时给出精确的「当前 / 上限」
            event.getToolTip().add(Component.translatable("superstack.tooltip.count",
                    CountFormat.exact(count),
                    CountFormat.exact(event.getItemStack().getMaxStackSize())).withStyle(ChatFormatting.GRAY));
        }
    }

    private static boolean superstack$isSophisticatedScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) {
            return false;
        }
        // 精妙背包 / 精妙存储 / 精妙核心的界面自带数量显示
        return minecraft.screen.getClass().getName().startsWith("net.p3pp3rf1y.sophisticated");
    }
}
