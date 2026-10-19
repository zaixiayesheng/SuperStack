package com.zaixiayesheng.superstack.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 应用能源（AE2）的内部库存。
 *
 * <p>AppEngInternalInventory 在构造时把每个槽的上限填成 64（或调用方给的值），
 * ME 网络的面板、接口、箱子都走这里，所以网络里的大堆叠会被截回 64。
 */
@Mixin(targets = "appeng.util.inv.AppEngInternalInventory", remap = false)
public abstract class Ae2AppEngInternalInventoryMixin {
    @ModifyReturnValue(method = "getSlotLimit(I)I", at = @At("RETURN"))
    private int superstack$raiseSlotLimit(int original) {
        return SuperStackConfig.raiseCompatLimit(original);
    }
}
