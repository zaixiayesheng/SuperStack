package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
        "com.simibubi.create.content.logistics.crate.CreativeCrateMountedStorage",
        "com.simibubi.create.content.fluids.drain.ItemDrainItemHandler"
}, remap = false)
public class CreateSlotLimitMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true)
    private void superstack$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.raiseCompatLimit(SuperStackConfig.CompatPatch.CREATE, cir.getReturnValue()));
    }
}
