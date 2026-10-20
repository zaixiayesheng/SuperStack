package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cucumber is the shared library behind Mystical Agriculture and its add-ons. Its item handler defaults every
 * slot to 64, so the infuser and friends reject anything bigger even though the item itself can stack higher.
 * Only the default (64) is raised; slots with an explicit limit keep theirs.
 */
@Mixin(targets = "com.blakebr0.cucumber.inventory.BaseItemStackHandler", remap = false)
public abstract class CucumberBaseItemStackHandlerMixin {
    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true, remap = false)
    private void superstack$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.raiseCompatLimit(cir.getReturnValue()));
    }
}
