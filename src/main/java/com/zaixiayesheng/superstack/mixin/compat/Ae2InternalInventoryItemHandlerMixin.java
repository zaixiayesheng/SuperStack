package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 应用能源 2（AE2）给 Forge 用的库存适配器。
 *
 * <p>{@code InternalInventoryItemHandler} 把 {@code getSlotLimit} 直接委托给内部库存，
 * 内部库存那边已经补过，这里再兜一层：万一某个实现自己返回 64/99，也能抬起来。
 */
@Mixin(targets = "appeng.api.inventories.InternalInventoryItemHandler", remap = false)
public abstract class Ae2InternalInventoryItemHandlerMixin {
    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true)
    private void superstack$raiseLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.raiseCompatLimit(SuperStackConfig.CompatPatch.AE2, cir.getReturnValue()));
    }
}
