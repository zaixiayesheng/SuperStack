package com.zaixiayesheng.superstack.mixin.compat;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 精妙（存储、背包、储罐）有自己的一套拖拽分摊算法，不走原版的
 * {@code AbstractContainerMenu#getQuickCraftPlaceCount}，而它同样用 float 做除法。
 *
 * <p>超过 16,777,216 的数量会被 float 舍入，拖两格时每格多给 1 个，两份加起来比原来多 1。
 * 这里改成整数除法，余数留在鼠标上。
 */
@Mixin(value = net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase.class, remap = false)
public abstract class SophisticatedQuickCraftMixin {

    @Inject(
            method = "getQuickCraftPlaceCount(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/item/ItemStack;)I",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void superstack$integerSplit(Slot slot, int slotsSize, int type, ItemStack carried,
                                                CallbackInfoReturnable<Integer> cir) {
        if (type == 0 && slotsSize > 0) {
            cir.setReturnValue(Math.min(slot.getMaxStackSize(carried), carried.getCount() / slotsSize));
        }
    }
}
