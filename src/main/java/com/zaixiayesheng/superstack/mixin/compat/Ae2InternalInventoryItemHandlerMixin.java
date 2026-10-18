package com.zaixiayesheng.superstack.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 应用能源（AE2）暴露给 Forge 物品处理器的视图。
 *
 * <p>它把上限直接委托给内部库存，而 {@code InternalInventory#getSlotLimit} 的默认值又是 99，
 * 匿名库存不经过 AppEngInternalInventory，就会卡在 99。
 */
@Mixin(targets = "appeng.api.inventories.InternalInventoryItemHandler", remap = false)
public abstract class Ae2InternalInventoryItemHandlerMixin {
    @ModifyReturnValue(method = "getSlotLimit(I)I", at = @At("RETURN"))
    private int superstack$raiseSlotLimit(int original) {
        return SuperStackConfig.raiseLegacyDefault(original);
    }
}
