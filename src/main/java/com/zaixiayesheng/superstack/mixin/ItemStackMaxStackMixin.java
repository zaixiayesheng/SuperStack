package com.zaixiayesheng.superstack.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A stack must never report a maximum smaller than the number of items it already carries.
 *
 * <p>Vanilla menu code assumes {@code getMaxStackSize() >= getCount()} and computes headroom as
 * {@code max - count}. When a stack was created while a larger limit was configured (or by a creative
 * action) and the limit is later lowered, that subtraction goes negative: {@code grow(negative)} shrinks the
 * slot while {@code shrink(negative)} grows the carried stack. That is exactly the "split and put both halves
 * back, one half disappears / one extra appears" symptom. Raising the reported max to at least the current
 * count makes every such subtraction non-negative instead of clamping or dropping items.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMaxStackMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void superstack$neverBelowCount(CallbackInfoReturnable<Integer> cir) {
        int count = ((ItemStack) (Object) this).getCount();
        int max = cir.getReturnValue();
        if (count > max) {
            cir.setReturnValue(count);
        }
    }
}
