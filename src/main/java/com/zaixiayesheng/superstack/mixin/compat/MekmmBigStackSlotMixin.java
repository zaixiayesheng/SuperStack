package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mekanism: MoreMachine exposes a few "big stack" slots that multiply the parent limit by 8 with
 * {@link Math#multiplyExact(int, int)}. Once the stack limit is raised past about 268 million that
 * multiplication overflows and throws, which surfaces as a crash when a machine is opened or updated.
 * Saturating the result keeps the slot usable at any configured limit.
 */
@Mixin(targets = {
        "com.jerry.meklm.common.inventory.slot.BigStackInputInventorySlot",
        "com.jerry.meklm.common.inventory.slot.BigStackOutputInventorySlot"
}, remap = false)
public abstract class MekmmBigStackSlotMixin {
    @Redirect(
            method = "getLimit(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;multiplyExact(II)I"),
            remap = false
    )
    private int superstack$saturatingMultiply(int left, int right) {
        long result = (long) left * (long) right;
        return (int) Math.min(result, (long) Integer.MAX_VALUE);
    }

    /**
     * Keep the x8 slots within the configured stack size. A slot holding more than one item stack's worth
     * would be written to NBT with a count the item stack codec rejects, so the surplus would be lost on
     * reload.
     */
    @Inject(method = "getLimit(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true, remap = false)
    private void superstack$capToStackSize(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.mekmmSlotLimit(cir.getReturnValue()));
    }
}
