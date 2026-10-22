package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import ironfurnaces.tileentity.TileEntityInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Iron Furnaces container limit.
 *
 * <p>{@code TileEntityInventory#setItem} clamps the incoming stack with {@code getMaxStackSize()},
 * which delegates to the {@code WorldlyContainer} default of 64, so a 32768 stack put into a
 * furnace is cut down to 64. Only that single call site is redirected; the method itself must keep
 * returning 64 because {@code TileEntityInventory#load} uses it as a <em>list size</em>
 * ({@code NonNullList.withSize(getMaxStackSize(), ...)}), and a larger value makes chunk loading
 * allocate an array of that many entries and blow up with {@code OutOfMemoryError}.
 *
 * <p>Methods are referenced by their production (SRG) names because mod classes are not remapped in
 * a production environment.
 */
@Mixin(value = ironfurnaces.tileentity.TileEntityInventory.class, remap = false)
public abstract class IronFurnacesInventoryLimitMixin {

    @Redirect(
        method = "m_6836_(ILnet/minecraft/world/item/ItemStack;)V",
        at = @At(
            value = "INVOKE",
            target = "Lironfurnaces/tileentity/TileEntityInventory;m_6893_()I"
        ),
        require = 0,
        remap = false
    )
    private int superstack$raiseSetItemClamp(TileEntityInventory instance) {
        return SuperStackConfig.ironFurnaceLimit(SuperStackConfig.VANILLA_MAX_STACK_SIZE);
    }
}
