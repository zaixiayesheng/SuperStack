package com.zaixiayesheng.superstack.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 精妙（Sophisticated Core）的库存槽位上限。
 *
 * <p>InventoryHandler 覆盖了 getSlotLimit，返回「基础槽位上限」——默认 64，
 * 装上堆叠升级会按倍率放大（128、256…）。这里取「原值」和「我们的上限」里大的那个，
 * 既把没升级时的 64 抬到 32768，也不会把升级后的 128 反而压小。
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler", remap = false)
public abstract class SophisticatedInventoryHandlerMixin {
    @ModifyReturnValue(method = "getSlotLimit(I)I", at = @At("RETURN"))
    private int superstack$raiseSlotLimit(int original) {
        return SuperStackConfig.raiseModdedSlotLimit(original);
    }
}
