package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Iron Furnaces hardcodes an output ceiling of 64 in the smelting checks; with a raised stack size
 * that ceiling must follow the configured limit, otherwise the output slot never fills up.
 *
 * <p>Only the smelting capacity constants are touched: the 64 inside {@code smelt}/{@code factorySmelt}
 * is the "million furnace smelts 64 at a time" multiplier and must stay as is, and the two 64s in
 * {@code tick} are the auto-IO thresholds. {@code tick} is also a static method, so a non-static
 * callback cannot target it at all.
 */
@Mixin(targets = "ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase", remap = false)
public abstract class IronFurnacesFurnaceTileMixin {

    @ModifyConstant(
            method = {"canSmelt", "smeltItemMult", "smeltFactoryItemMult"},
            constant = @Constant(intValue = 64),
            remap = false)
    private int superstack$raiseOutputCeiling(int original) {
        return SuperStackConfig.maxStackSize();
    }
}