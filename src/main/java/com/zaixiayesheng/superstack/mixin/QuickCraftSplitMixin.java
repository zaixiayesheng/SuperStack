package com.zaixiayesheng.superstack.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * 拖动分摊（按住左键把一堆拖到几个格子里）的分摊数量。
 *
 * <p>原版用 {@code Mth.floor((float) count / (float) slots.size())}：float 只有 24 位尾数，超过
 * 16,777,216 的数量会被舍入到 2 的幂的倍数。1,073,741,823（2^30 - 1）拖两格时每格会被舍成
 * 536,870,912，两份加起来 1,073,741,824 —— 每次拖拽凭空多出 1 个。改成整数除法后两份之和恰好等于原数量。
 *
 * <p>1.20.1 与 1.21.1 的这个方法签名一致（1.20.1 字节码里同样是 {@code fdiv} + {@code Mth.floor(F)}），
 * 所以两个分支共用同一份实现。
 */
@Mixin(AbstractContainerMenu.class)
public abstract class QuickCraftSplitMixin {

    @Inject(method = "getQuickCraftPlaceCount", at = @At("HEAD"), cancellable = true)
    private static void superstack$integerSplit(Set<Slot> slots, int type, ItemStack stack,
                                                CallbackInfoReturnable<Integer> cir) {
        if (type == 0 && !slots.isEmpty()) {
            cir.setReturnValue(stack.getCount() / slots.size());
        }
    }
}
