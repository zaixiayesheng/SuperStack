package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Iron Furnaces caps the output slot at 64 in its smelting checks; with a raised stack size that
 * ceiling has to follow the configured limit or the output slot never fills past 64.
 *
 * <p>Three constants are touched: canSmelt (min(maxStackSize, 64)), and
 * smeltItemMult / smeltFactoryItemMult ((64 - out.getCount()) / result.getCount()).
 * The 64 inside smelt/factorySmelt is the "million furnace smelts 64 at a time" multiplier and must
 * stay untouched, and the two 64s in tick are auto-IO thresholds. tick is static as well, so a
 * non-static callback cannot target it.
 */
@Mixin(targets = "ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase", remap = false)
public abstract class IronFurnacesFurnaceTileMixin {

    @ModifyConstant(
            method = {"canSmelt", "smeltItemMult", "smeltFactoryItemMult"},
            constant = @Constant(intValue = 64),
            remap = false)
    private int superstack$raiseOutputCeiling(int original) {
        return SuperStackConfig.compatStackLimit();
    }
}