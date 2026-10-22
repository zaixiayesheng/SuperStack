package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
        "appeng.api.inventories.ContainerAdapter"
}, remap = false)
public class Ae2ContainerAdapterMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void superstack$raiseMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.raiseCompatLimit(SuperStackConfig.CompatPatch.AE2, cir.getReturnValue()));
    }
}
