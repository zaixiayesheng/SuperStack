package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackCraftLimiter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 玩家背包里那个 2×2 的合成格，路径与工作台一样，同样封顶。 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void superstack$limitCraftLoop(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index == 0 && SuperStackCraftLimiter.reachedLimit()) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
