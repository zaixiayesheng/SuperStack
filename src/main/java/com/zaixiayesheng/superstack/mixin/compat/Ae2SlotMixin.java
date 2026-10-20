package com.zaixiayesheng.superstack.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 应用能源（AE2）界面里的槽位。
 *
 * <p>AE2 的菜单槽位自己算上限，界面上的「能放多少个」由它决定，不和原版 Slot 一致。
 */
@Mixin(targets = "appeng.menu.slot.AppEngSlot", remap = false)
public abstract class Ae2SlotMixin {
    @ModifyReturnValue(method = "getMaxStackSize()I", at = @At("RETURN"))
    private int superstack$raiseSlotLimit(int original) {
        return SuperStackConfig.raiseCompatLimit(SuperStackConfig.CompatPatch.AE2, original);
    }
}
