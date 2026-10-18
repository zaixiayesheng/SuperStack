package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 精妙核心（Sophisticated Core）的库存分区。
 *
 * <p>{@code InventoryHandler#getSlotLimit(int)} 返回分区自己的 baseSlotLimit（默认 64），
 * 它盖掉了 Forge {@code ItemStackHandler} 的同名方法，所以只补 ItemStackHandler 够不着。
 *
 * <p>这里用「只升不降」：精妙的堆叠升级会把槽位上限抬到 128、256，
 * 若按 64→配置值映射反而会把升级结果压小。
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler", remap = false)
public abstract class SophisticatedInventoryHandlerMixin {
    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true)
    private void superstack$raiseLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.raiseModdedSlotLimit(cir.getReturnValue()));
    }
}
