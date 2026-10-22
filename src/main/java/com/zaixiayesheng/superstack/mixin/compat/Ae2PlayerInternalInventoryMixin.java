package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import appeng.util.inv.PlayerInternalInventory;
import org.spongepowered.asm.mixin.Mixin;

/**
 * AE2 player inventory adapter.
 *
 * <p>AE2 wraps the player inventory as an {@code InternalInventory} but does not override
 * {@code getSlotLimit}, so it falls back to the interface default of 99. Every insert made through
 * that adapter - most visibly the "craft to player inventory" path of the crafting terminal - is
 * therefore split into stacks of 99. Raising the reported limit lets those merges fill a stack up to
 * the configured size instead.
 *
 * <p>The method does not exist on the target class, only as an interface default, so it is merged in
 * as a real override instead of being injected into an inherited body.
 */
@Mixin(value = appeng.util.inv.PlayerInternalInventory.class, remap = false)
public abstract class Ae2PlayerInternalInventoryMixin {
    public int getSlotLimit(int slot) {
        return SuperStackConfig.raiseCompatLimit(SuperStackConfig.CompatPatch.AE2, 99);
    }
}
