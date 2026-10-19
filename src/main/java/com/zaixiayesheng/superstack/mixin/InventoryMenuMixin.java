package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackBulkCraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shift-click on the 2x2 crafting result in the player inventory. */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Shadow
    @Final
    private CraftingContainer craftSlots;

    @Shadow
    @Final
    private ResultContainer resultSlots;

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void superstack$bulkCraft(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index != 0) {
            return;
        }
        if (SuperStackBulkCraft.tryCraftAll((InventoryMenu) (Object) this, this.craftSlots, this.resultSlots,
                player, 9, 45)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}