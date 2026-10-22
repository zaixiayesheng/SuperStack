package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 奥瑞科技把机器槽位上限写死成 64，超大堆叠塞不进去。
 */
@Mixin(targets = "rearth.oritech.api.item.containers.SimpleInventoryStorage", remap = false)
public class OritechInventoryStorageMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true)
    private void superstack$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.oritechSlotLimit(cir.getReturnValue()));
    }
}
