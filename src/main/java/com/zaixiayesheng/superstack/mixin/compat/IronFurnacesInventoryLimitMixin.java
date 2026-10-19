package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Iron Furnaces container limit.
 *
 * <p>{@code TileEntityInventory} implements {@code WorldlyContainer} and overrides
 * {@code getMaxStackSize()} by delegating to the interface default (64). That value feeds both the
 * menu slots and the Forge item handler wrapper, so a 32768 stack inserted into a furnace is cut
 * down to 64. The override is raised to the configured limit.
 *
 * <p>The method is referenced by its production (SRG) name because mod classes are not remapped in
 * a production environment.
 */
@Mixin(targets = "ironfurnaces.tileentity.TileEntityInventory", remap = false)
public abstract class IronFurnacesInventoryLimitMixin {

    @Inject(method = "m_6893_()I", at = @At("RETURN"), cancellable = true, remap = false)
    private void superstack$raiseInventoryLimit(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.maxStackSize());
    }
}