package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 应用能源 2（AE2）的内部库存接口。
 *
 * <p>{@code InternalInventory#getSlotLimit(int)} 的默认实现直接返回 99；
 * 玩家背包 / 光标那两种适配器（{@code PlayerInternalInventory}、{@code CarriedItemInventory}）
 * 都没有重写它，于是合成终端的产物一进背包就被拆成 99 一组。
 *
 * <p>这里补的是接口默认方法本身，所有没重写的实现都会跟着变；
 * 自己重写了上限的（比如合成格的 {@code AppEngInternalInventory}）另有专门的处理。
 */
@Mixin(targets = "appeng.api.inventories.InternalInventory", remap = false)
public abstract class Ae2InternalInventoryMixin {
    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true)
    private void superstack$raiseLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.raiseLegacyDefault(cir.getReturnValue()));
    }
}
