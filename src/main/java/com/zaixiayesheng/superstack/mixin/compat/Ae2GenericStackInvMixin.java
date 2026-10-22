package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import appeng.helpers.externalstorage.GenericStackInv;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AE2 的通用堆叠库存（ME 接口的存储区、图样供应器等都基于它）。
 *
 * <p>{@code getMaxAmount(key)} 对物品用的是 {@code min(AEItemKey.getMaxStackSize(), 容量)}，
 * 而 {@code AEItemKey} 在构造时就把物品上限缓存下来了。如果这个键是在本模组改上限之前建的
 * （或者来自一个默认上限仍是 64/99 的物品），接口每个槽就只收 64/99 个，9 个槽正好 891 个，
 * 表现成「管道/卸货插件搬不进去，手动放进终端却没问题」。
 *
 * <p>这里把这种「原版默认值」抬到配置上限；物品键本身缓存了更大值（说明已经是我们改过的物品）时保持不动。
 */
@Mixin(value = appeng.helpers.externalstorage.GenericStackInv.class, remap = false)
public abstract class Ae2GenericStackInvMixin {

    @Inject(method = "getMaxAmount(Lappeng/api/stacks/AEKey;)J", at = @At("RETURN"), cancellable = true)
    private void superstack$raiseSlotAmount(CallbackInfoReturnable<Long> cir) {
        long original = cir.getReturnValue();
        if (original <= 0L || original > 99L) {
            return;
        }
        cir.setReturnValue((long) SuperStackConfig.raiseLegacyDefault((int) original));
    }
}
