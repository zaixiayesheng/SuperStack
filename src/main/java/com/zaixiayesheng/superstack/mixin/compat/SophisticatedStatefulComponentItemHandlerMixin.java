package com.zaixiayesheng.superstack.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 精妙（Sophisticated Core）合成升级的合成格上限。
 *
 * <p>合成升级内部那 9 格合成格由 StatefulComponentItemHandler 托管，它的 getSlotLimit 直接写死
 * 返回 99，所以即使物品堆叠上限已经被抬到 32768，合成格里每次也只能放 99 个。
 * 这里把 99 换成配置的上限，其它显式数值原样保留。
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.StatefulComponentItemHandler", remap = false)
public abstract class SophisticatedStatefulComponentItemHandlerMixin {
    @ModifyReturnValue(method = "getSlotLimit(I)I", at = @At("RETURN"))
    private int superstack$raiseSlotLimit(int original) {
        return SuperStackConfig.raiseLegacyDefault(original);
    }
}
