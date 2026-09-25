package com.zaixiayesheng.superstack.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 原版 {@link Container#getMaxStackSize()} 的默认返回值是 99，合成格 / 箱子 / 背包里
 * 那些没有自己重写这个方法的容器全都吃这个默认值，所以堆叠上限提上去以后，
 * 这些容器还是只能放 99 个。
 *
 * 这里只把 99 换成配置的上限，其它值（各模组自己设的槽位上限）原样保留。
 */
@Mixin(Container.class)
public interface ContainerMixin {
    @ModifyReturnValue(method = "getMaxStackSize", at = @At("RETURN"))
    private int superstack$raiseVanillaLimit(int original) {
        return SuperStackConfig.raiseVanillaDefault(original);
    }
}
