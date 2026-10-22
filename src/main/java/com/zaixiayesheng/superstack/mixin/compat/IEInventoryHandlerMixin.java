package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 沉浸工程暴露给自动化的物品处理器把每格上限写死成 64，插入时还会再看一次机器的槽位上限。
 */
@Mixin(targets = "blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler", remap = false)
public class IEInventoryHandlerMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true)
    private void superstack$slotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.ieSlotLimit(cir.getReturnValue()));
    }

    @ModifyExpressionValue(
            method = {"insertItem", "extractItem"},
            at = @At(value = "INVOKE",
                    target = "Lblusunrize/immersiveengineering/common/util/inventory/IIEInventory;getSlotLimit(I)I"))
    private int superstack$machineSlotLimit(int original) {
        return SuperStackConfig.ieSlotLimit(original);
    }
}
