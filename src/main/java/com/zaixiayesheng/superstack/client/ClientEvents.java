package com.zaixiayesheng.superstack.client;

import com.zaixiayesheng.superstack.CountFormat;
import com.zaixiayesheng.superstack.SuperStack;
import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 客户端表现：提示框里给出精确数量。只在客户端加载（{@link Dist#CLIENT}）。 */
@Mod.EventBusSubscriber(modid = SuperStack.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!SuperStackConfig.tooltipExactCount()) {
            return;
        }
        int count = event.getItemStack().getCount();
        if (count > 99) {
            // 槽位里可能是缩写（12.3k），悬停时给出精确数量
            event.getToolTip().add(Component.translatable("superstack.tooltip.count",
                    CountFormat.exact(count)).withStyle(ChatFormatting.GRAY));
        }
    }
}
