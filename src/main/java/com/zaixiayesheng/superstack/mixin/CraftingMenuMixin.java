package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackCraftLimiter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 工作台的「Shift + 点击结果格」会一直合成到背包放不下，大堆叠下就是上万次配方查询。
 * 到上限后返回空，外层 while 立刻结束，结果格与材料不动。
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void superstack$limitCraftLoop(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index == 0 && SuperStackCraftLimiter.reachedLimit()) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
