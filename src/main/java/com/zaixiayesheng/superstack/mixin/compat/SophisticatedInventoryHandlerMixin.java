package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sophisticated Core storage slots (backpacks, chests, barrels) clamp insertions to the base slot
 * limit (64) regardless of the item's own max stack size, so large stacks get split.
 *
 * <p>Both entry points are covered: {@code getSlotLimit(int)} (what the GUI slot asks for) and
 * {@code getStackLimit(int, ItemStack)} (what the inventory handler uses while inserting).
 *
 * <p>Only the legacy defaults (64 / 99) are remapped; explicit limits such as 1-slot upgrade slots
 * or limits raised by a Stack Upgrade are left alone.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler", remap = false)
public abstract class SophisticatedInventoryHandlerMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true)
    private void superstack$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.raiseLegacyDefault(cir.getReturnValue()));
    }

    @Inject(method = "getStackLimit(ILnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    private void superstack$raiseStackLimit(int slot, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.raiseLegacyDefault(cir.getReturnValue()));
    }
}