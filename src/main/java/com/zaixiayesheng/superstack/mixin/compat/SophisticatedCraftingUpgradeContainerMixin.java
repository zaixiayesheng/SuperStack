package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sophisticated Core runs every shift-click craft as a separate iteration of a
 * loop in {@code StorageContainerMenuBase#clicked}. With our large stacks that
 * loop can run tens of thousands of times inside a single tick and freeze the
 * server, so the number of repeats per click is capped by {@code maxCraftsPerClick}.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer", remap = false)
public class SophisticatedCraftingUpgradeContainerMixin {
    @Inject(method = "getRepeatedQuickMoveLimit", at = @At("RETURN"), cancellable = true, remap = false)
    private void superstack$capRepeatedQuickMoves(Slot slot, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        int limit = cir.getReturnValue();
        int cap = SuperStackConfig.maxCraftsPerClick();
        // 0 means "no limit" in the original, which is exactly the case that freezes.
        if (limit <= 0 || limit > cap) {
            cir.setReturnValue(cap);
        }
    }
}
