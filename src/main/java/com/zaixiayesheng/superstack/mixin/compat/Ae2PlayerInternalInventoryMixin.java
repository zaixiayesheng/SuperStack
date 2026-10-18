package com.zaixiayesheng.superstack.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * AE2 player inventory adapter.
 *
 * <p>AE2 wraps the player inventory as an {@code InternalInventory} but does not override
 * {@code getSlotLimit}, so it falls back to the interface default of 99. Every insert made through
 * that adapter - most visibly the "craft to player inventory" path of the crafting terminal - is
 * therefore split into stacks of 99. Raising the reported limit lets those merges fill a stack up to
 * the configured size instead.
 */
@Mixin(targets = "appeng.util.inv.PlayerInternalInventory", remap = false)
public abstract class Ae2PlayerInternalInventoryMixin {
    @ModifyReturnValue(method = "getSlotLimit(I)I", at = @At("RETURN"))
    private int superstack$raiseSlotLimit(int original) {
        return SuperStackConfig.raiseLegacyDefault(original);
    }
}
