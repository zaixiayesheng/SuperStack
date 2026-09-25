package com.zaixiayesheng.superstack.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * {@link Slot#getMaxStackSize()} 直接取容器的上限。有些容器会自己重写
 * {@code Container#getMaxStackSize()}，光补接口默认方法盖不住，所以这里再兜一层：
 * 只要拿到的还是原版的 99，就换成配置上限。
 *
 * 注意 {@code getMaxStackSize(ItemStack)} 是 min(槽位上限, 物品上限)，
 * 它会在本方法之后计算，不用另外处理。
 */
@Mixin(Slot.class)
public abstract class SlotMixin {
    @ModifyReturnValue(method = "getMaxStackSize()I", at = @At("RETURN"))
    private int superstack$raiseVanillaLimit(int original) {
        return SuperStackConfig.raiseVanillaDefault(original);
    }
}
