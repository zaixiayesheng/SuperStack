package com.zaixiayesheng.superstack.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * AE2 把方块形态的通用堆叠库存（ME 接口、图样供应器……）通过 {@code GenericStackItemStorage}
 * 暴露成物品处理器给管道/卸货插件用。
 *
 * <p>它的 {@code getSlotLimit(slot)} 直接返回 {@code GenericStackInv.getCapacity(物品类型)}，
 * 这个容量由 {@code GenericSlotCapacities} 注册，默认就是原版的 64（部分情况下 99）。
 * 于是手动在界面里能塞到上限，管道却只按 64/99 往里灌。</p>
 */
@Mixin(targets = "appeng.helpers.externalstorage.GenericStackItemStorage", remap = false)
public abstract class Ae2GenericStackItemStorageMixin {
    @ModifyReturnValue(method = "getSlotLimit(I)I", at = @At("RETURN"))
    private int superstack$raiseSlotLimit(int original) {
        return SuperStackConfig.raiseLegacyDefault(original);
    }
}
