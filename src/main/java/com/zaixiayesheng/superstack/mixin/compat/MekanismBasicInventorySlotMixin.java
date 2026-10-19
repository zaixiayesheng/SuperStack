package com.zaixiayesheng.superstack.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 通用机械（Mekanism）的槽位上限。
 *
 * <p>BasicInventorySlot 把默认上限固定成 99，机器界面、输入输出槽都从它取上限，
 * 所以物品组件改成 32768 之后机器里还是只能堆 99 个。
 *
 * <p>只替换返回 99 或 64 的情况：照旧尊重源码里显式写死的小上限（比如 1 个的槽）。
 */
@Mixin(targets = "mekanism.common.inventory.slot.BasicInventorySlot", remap = false)
public abstract class MekanismBasicInventorySlotMixin {
    @ModifyReturnValue(method = "getLimit(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"))
    private int superstack$raiseSlotLimit(int original) {
        return SuperStackConfig.raiseCompatLimit(original);
    }
}
