package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.ItemMaxStackAccess;
import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.20.1 的堆叠上限是 {@link Item} 上的私有字段 {@code maxStackSize}，
 * {@code getMaxStackSize()} 直接把它返回，而且方法是 final、所有子类共用。
 *
 * <p>所以只接管这一个方法就够了：原版 64 的物品拿到缩放后的上限，
 * 原本只能堆 1 个的（工具、桶）按阈值原样返回。
 */
@Mixin(Item.class)
public abstract class ItemMixin implements ItemMaxStackAccess {
    @Shadow
    @Final
    private int maxStackSize;

    @Override
    public int superstack$vanillaMaxStackSize() {
        return this.maxStackSize;
    }

    @Inject(method = "getMaxStackSize()I", at = @At("HEAD"), cancellable = true)
    private void superstack$raiseMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.scaledMaxForItem(this.maxStackSize));
    }
}
