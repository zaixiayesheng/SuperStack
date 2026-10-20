package com.zaixiayesheng.superstack.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 功能性存储（Functional Storage）的抽屉容量。
 *
 * <p>抽屉上限是「格数 × 这一叠有多少个 ÷ 64」，所以物品堆叠抬到 32768 之后，
 * 同一个 2000 容量的抽屉会显示成 1,024,000，而且空抽屉和有物品的抽屉算出来的数还不一样，
 * 看起来像是容量自己往上长。
 *
 * <p>默认按原版基准算：一份 = 64 个，容量保持不变。配置 functionalStorageVanillaCapacity 可以关掉。
 * 创造抽屉和那个虚拟槽原本就返回 Integer.MAX_VALUE，这里不动。
 */
@Mixin(targets = "com.buuz135.functionalstorage.inventory.BigInventoryHandler", remap = false)
public abstract class FunctionalStorageBigInventoryHandlerMixin {
    @Shadow
    public abstract double getTotalAmount();

    @ModifyReturnValue(method = "getSlotLimit(I)I", at = @At("RETURN"))
    private int superstack$vanillaCapacity(int original) {
        return superstack$adjustCapacity(original);
    }

    @ModifyReturnValue(method = "getSlotLimit(ILnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"))
    private int superstack$vanillaCapacityWithStack(int original) {
        return superstack$adjustCapacity(original);
    }

    private int superstack$adjustCapacity(int original) {
        if (!SuperStackConfig.functionalStorageVanillaCapacity() || original == Integer.MAX_VALUE) {
            return original;
        }
        double total = this.getTotalAmount();
        if (total <= 0.0) {
            return original;
        }
        return (int) Math.floor(total);
    }
}
