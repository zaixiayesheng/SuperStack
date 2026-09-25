package com.zaixiayesheng.superstack.client;

import com.zaixiayesheng.superstack.SuperStack;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** 客户端表现：提示框里给出精确数量。由 {@link SuperStackClient} 注册。 */
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!com.zaixiayesheng.superstack.SuperStackConfig.tooltipExactCount()) {
            return;
        }
        int count = event.getItemStack().getCount();
        if (count > 99) {
            // 槽位里可能是缩写（12.3k），悬停时给出精确数量
            event.getToolTip().add(Component.translatable("superstack.tooltip.count",
                    com.zaixiayesheng.superstack.CountFormat.exact(count)).withStyle(ChatFormatting.GRAY));
        }
    }
}
