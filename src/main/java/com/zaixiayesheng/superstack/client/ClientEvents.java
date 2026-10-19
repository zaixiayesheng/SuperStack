package com.zaixiayesheng.superstack.client;

import com.zaixiayesheng.superstack.CountFormat;
import com.zaixiayesheng.superstack.SuperStack;
import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client side: the tooltip shows "current / limit".
 *
 * <p>Sophisticated's own screens and tooltips already show the count, so nothing is added there.
 *
 * <p>{@code bus = FORGE} is required on 1.20.1: the default bus is the mod bus, and
 * {@link ItemTooltipEvent} never fires there.
 */
@Mod.EventBusSubscriber(modid = SuperStack.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
            // The slot may show an abbreviation (12.3k); hovering gives the exact current / limit.
            event.getToolTip().add(Component.translatable("superstack.tooltip.count",
                    CountFormat.exact(count),
                    CountFormat.exact(superstack$displayLimit(event.getItemStack()))).withStyle(ChatFormatting.GRAY));
        }
    }

    private static boolean superstack$isSophisticatedScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) {
            return false;
        }
        // Sophisticated backpacks / storage / core screens draw their own counts.
        return minecraft.screen.getClass().getName().startsWith("net.p3pp3rf1y.sophisticated");
    }

    /**
     * 提示框里显示的「上限」。精妙等模组会把某些堆叠的上限设成 Integer.MAX_VALUE（表示不限），
     * 直接显示会变成 2,147,483,647，没有意义；这里统一压到配置上限。
     */
    private static int superstack$displayLimit(ItemStack stack) {
        int stackLimit = stack.getMaxStackSize();
        int configured = SuperStackConfig.maxStackSize();
        return stackLimit > configured ? configured : stackLimit;
    }
}