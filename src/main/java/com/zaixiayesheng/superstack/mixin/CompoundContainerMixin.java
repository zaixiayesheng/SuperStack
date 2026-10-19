package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.CompoundContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Double chest / compound container: report the configured limit instead of the wrapped container default. */
@Mixin(CompoundContainer.class)
public abstract class CompoundContainerMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void superstack$raiseLimit(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.maxStackSize());
    }
}