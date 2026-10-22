package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 机器界面使用的容器把 getMaxStackSize 写死成 64，导致界面槽位与 Shift 点击都只认 64。
 */
@Mixin(targets = "blusunrize.immersiveengineering.common.gui.BlockEntityInventory", remap = false)
public class IEBlockEntityInventoryMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void superstack$maxStackSize(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.ieSlotLimit(cir.getReturnValue()));
    }
}
