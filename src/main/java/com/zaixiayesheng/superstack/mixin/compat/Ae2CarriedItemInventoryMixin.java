package com.zaixiayesheng.superstack.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * AE2 carried stack ("item on the cursor") adapter.
 *
 * <p>Like {@code PlayerInternalInventory}, this adapter relies on the {@code InternalInventory}
 * default slot limit of 99, which caps "craft onto cursor" and "craft a stack" in the crafting
 * terminal at 99 items per stack.
 */
@Mixin(targets = "appeng.util.inv.CarriedItemInventory", remap = false)
public abstract class Ae2CarriedItemInventoryMixin {
    @ModifyReturnValue(method = "getSlotLimit(I)I", at = @At("RETURN"))
    private int superstack$raiseSlotLimit(int original) {
        return SuperStackConfig.raiseLegacyDefault(original);
    }
}
