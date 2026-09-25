package com.zaixiayesheng.superstack.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge 的 ItemStackHandler（很多模组的机器槽位用它）。
 * 只有配置里打开 raiseModdedSlotLimits 才生效，而且只升不降 —— 直接改成
 * 一个天文数字会让别的模组的插入判断出问题（有的模组就是这么被搞坏的）。
 */
@Mixin(ItemStackHandler.class)
public abstract class ItemStackHandlerMixin {
    @ModifyReturnValue(method = "getSlotLimit", at = @At("RETURN"))
    private int superstack$raiseModdedLimit(int original) {
        return SuperStackConfig.raiseModdedSlotLimit(original);
    }
}
