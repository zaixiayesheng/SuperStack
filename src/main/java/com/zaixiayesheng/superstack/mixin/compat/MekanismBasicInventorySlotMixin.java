package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 通用机械（Mekanism）的槽位上限。
 *
 * <p>{@code BasicInventorySlot} 把默认上限写死成 99
 * （{@code getLimit(ItemStack)} 里 {@code Math.min(99, stack.getMaxStackSize())}），
 * 机器界面里的槽位、量子储物等全都走它，所以不补这里，通用机械的机器永远只能放 99 个。
 *
 * <p>只替换「正好 64 / 99」的老默认值，1、16、1024 这类明确的小上限原样保留。
 */
@Mixin(targets = "mekanism.common.inventory.slot.BasicInventorySlot", remap = false)
public abstract class MekanismBasicInventorySlotMixin {
    @Inject(method = "getLimit(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    private void superstack$raiseLimit(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.raiseLegacyDefault(cir.getReturnValue()));
    }
}
