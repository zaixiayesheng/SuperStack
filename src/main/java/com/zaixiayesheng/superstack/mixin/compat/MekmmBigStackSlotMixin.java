package com.zaixiayesheng.superstack.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mekanism: MoreMachine exposes a few "big stack" slots that multiply the parent limit by 8 with
 * {@link Math#multiplyExact(int, int)}. Once the stack limit is raised past about 268 million that
 * multiplication overflows and throws, which surfaces as a crash when a machine is opened or updated.
 * Saturating the result keeps the slot usable at any configured limit.
 */
@Mixin(targets = {
        "com.jerry.meklm.common.inventory.slot.BigStackInputInventorySlot",
        "com.jerry.meklm.common.inventory.slot.BigStackOutputInventorySlot"
}, remap = false)
public abstract class MekmmBigStackSlotMixin {
    @Redirect(
            method = "getLimit(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;multiplyExact(II)I"),
            remap = false
    )
    private int superstack$saturatingMultiply(int left, int right) {
        long result = (long) left * (long) right;
        return (int) Math.min(result, (long) Integer.MAX_VALUE);
    }
}
