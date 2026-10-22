package com.zaixiayesheng.superstack.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * AE2 的通用堆叠库存（ME 接口的存储区、图样供应器等都基于它）。
 *
 * <p>{@code getMaxAmount(key)} 对物品用的是 {@code min(AEItemKey.getMaxStackSize(), 容量)}，
 * 而 {@code AEItemKey} 在构造时就把物品上限缓存下来了 —— 如果这个键是在本模组改上限之前
 * 建的（或者来自一个默认上限仍是 64/99 的物品），接口每个槽就只收 64/99 个，9 个槽正好
 * 891 个，表现成「管道/卸货插件搬不进去，手动放进终端却没问题」。</p>
 *
 * <p>这里把这种「原版默认值」抬到配置上限；物品键本身缓存了更大值（说明已经是我们改过的
 * 物品）时保持不动。</p>
 */
@Mixin(targets = "appeng.helpers.externalstorage.GenericStackInv", remap = false)
public abstract class Ae2GenericStackInvMixin {
    @ModifyReturnValue(method = "getMaxAmount(Lappeng/api/stacks/AEKey;)J", at = @At("RETURN"))
    private long superstack$raiseSlotAmount(long original) {
        if (original <= 0 || original > 99L) {
            return original;
        }
        return SuperStackConfig.raiseLegacyDefault((int) original);
    }
}
