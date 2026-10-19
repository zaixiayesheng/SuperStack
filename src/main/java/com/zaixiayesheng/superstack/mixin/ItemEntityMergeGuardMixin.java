package com.zaixiayesheng.superstack.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 地上的物品合并只处理 64 以内的小堆叠。
 *
 * <p>原版 {@code areMergable} 用 int 相加判断「两个堆叠加起来放不放得下」，合并本身每次只搬 64 个。
 * 堆叠上限开到 32 位整数上限后，两个大堆叠相加会溢出成负数，合并逻辑会把数量算错；
 * 这里直接把「加起来超过 64」的组合判为不可合并，掉落物仍然各自独立，行为与原版一致。
 */
@Mixin(ItemEntity.class)
public class ItemEntityMergeGuardMixin {

    @Inject(method = "areMergable(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void superstack$guardMerge(ItemStack destination, ItemStack origin,
                                              CallbackInfoReturnable<Boolean> cir) {
        if ((long) destination.getCount() + origin.getCount() > 64L) {
            cir.setReturnValue(false);
        }
    }
}
