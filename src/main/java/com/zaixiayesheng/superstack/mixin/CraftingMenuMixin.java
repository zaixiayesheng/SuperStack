package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackBulkCraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 工作台的「Shift + 点击结果格」：先把这一批直接合完（批量扣材料、批量塞背包），
 * 批量处理不了的配方回落到原版循环。
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

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
        if (SuperStackBulkCraft.tryCraftAll((CraftingMenu) (Object) this, this.craftSlots, this.resultSlots,
                player, 10, 46)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
    }
}
