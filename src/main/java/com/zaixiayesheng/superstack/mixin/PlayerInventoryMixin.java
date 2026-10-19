package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Player inventory limit.
 *
 * <p>On 1.20.1 {@code Container.getMaxStackSize()} defaults to 64 and {@code Inventory} does not
 * override it. {@code Inventory.hasRemainingSpaceForItem} and {@code Inventory.addResource} compare
 * against it directly, so a 32768 stack gets split into 64s (and the leftovers are dropped one 64
 * per entity when the inventory is full).
 *
 * <p>The calls are redirected instead of adding an override method: a method added by a mixin keeps
 * its literal name in a production (SRG) environment, so it would not override the interface default
 * at all. Redirect targets go through the refmap, so these are remapped correctly.
 */
@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {

    @Redirect(
            method = "hasRemainingSpaceForItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getMaxStackSize()I"),
            require = 0
    )
    private int superstack$limitForSpaceCheck(Inventory inventory) {
        return SuperStackConfig.maxStackSize();
    }

    @Redirect(
            method = "addResource(ILnet/minecraft/world/item/ItemStack;)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getMaxStackSize()I"),
            require = 0
    )
    private int superstack$limitForAddResource(Inventory inventory) {
        return SuperStackConfig.maxStackSize();
    }
}