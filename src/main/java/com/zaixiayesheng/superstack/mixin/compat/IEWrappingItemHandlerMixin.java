package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 包装处理器同样把上限夹在 64（储物桶、工具箱、机械臂一类走这里）。
 */
@Mixin(targets = "blusunrize.immersiveengineering.common.util.inventory.WrappingItemHandler", remap = false)
public class IEWrappingItemHandlerMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true)
    private void superstack$slotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.ieSlotLimit(cir.getReturnValue()));
    }
}
