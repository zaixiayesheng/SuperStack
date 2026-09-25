package com.zaixiayesheng.superstack.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import net.neoforged.neoforge.items.ComponentItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 同 {@link ItemStackHandlerMixin}，这个是挂在物品组件上的那种物品槽（背包、箱子等）。 */
@Mixin(ComponentItemHandler.class)
public abstract class ComponentItemHandlerMixin {
    @ModifyReturnValue(method = "getSlotLimit", at = @At("RETURN"))
    private int superstack$raiseModdedLimit(int original) {
        return SuperStackConfig.raiseModdedSlotLimit(original);
    }
}
