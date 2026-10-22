package com.zaixiayesheng.superstack.mixin.compat;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sophisticated Core (storage, backpacks, tanks) uses its own drag-splitting helper instead of
 * {@code AbstractContainerMenu#getQuickCraftPlaceCount}, and that helper divides the carried count by the
 * slot count in {@code float} as well. Above 16,777,216 items the float rounding hands every slot one item
 * too many, so dragging a billion-item stack across two slots creates an extra item.
 *
 * <p>Same fix as the vanilla path: divide in {@code int} and keep the remainder on the cursor.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase", remap = false)
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
