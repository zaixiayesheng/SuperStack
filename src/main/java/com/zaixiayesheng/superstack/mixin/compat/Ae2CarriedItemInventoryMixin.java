package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import appeng.util.inv.CarriedItemInventory;
import org.spongepowered.asm.mixin.Mixin;

/**
 * AE2 carried stack ("item on the cursor") adapter.
 *
 * <p>Like {@code PlayerInternalInventory}, this adapter relies on the {@code InternalInventory}
 * default slot limit of 99, which caps "craft onto cursor" and "craft a stack" in the crafting
 * terminal at 99 items per stack.
 *
 * <p>The method does not exist on the target class, only as an interface default, so it is merged in
 * as a real override instead of being injected into an inherited body.
 */
@Mixin(value = appeng.util.inv.CarriedItemInventory.class, remap = false)
public abstract class Ae2CarriedItemInventoryMixin {
    public int getSlotLimit(int slot) {
        return SuperStackConfig.raiseCompatLimit(SuperStackConfig.CompatPatch.AE2, 99);
    }
}
