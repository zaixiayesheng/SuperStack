package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 沉浸工程：物品处理器对外报告的槽位上限。
 *
 * <p>{@code IEInventoryHandler} 把上限写死成 64，自动化（漏斗、管道、总线）
 * 全部按这个数搬运。抬到配置上限后，插入时仍会被 {@code ItemStack#getMaxStackSize}
 * 二次夹取，所以不会超过物品自身的堆叠上限。</p>
 */
@Mixin(targets = "blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler", remap = false)
public class IEInventoryHandlerMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true)
    private void superstack$slotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.raiseCompatLimit(
                SuperStackConfig.CompatPatch.IMMERSIVE_ENGINEERING, cir.getReturnValue()));
    }
}
