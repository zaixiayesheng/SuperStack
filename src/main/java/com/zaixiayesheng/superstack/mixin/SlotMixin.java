package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 槽位上限。三层：
 *
 * <ol>
 *   <li>容器返回的原版 99 换成配置上限（有的容器自己重写，光补接口默认方法盖不住，所以在这一层再兜一次）；</li>
 *   <li>本槽的上限不小于它当前装着的数量——堵住「剩余空间 = 上限 - 已有数量」变成负数；
 *       只要装的是同一种物品才抬，不然槽里 1,000 个铁锭会让 16 个一组的鸡蛋也能塞 1,000；</li>
 *   <li>同理处理带 {@link ItemStack} 参数的重载（原版是 {@code min(槽位上限, 物品上限)}）。</li>
 * </ol>
 */
@Mixin(Slot.class)
public abstract class SlotMixin {

    @Inject(method = "getMaxStackSize()I", at = @At("RETURN"), cancellable = true)
    private void superstack$raiseVanillaLimit(CallbackInfoReturnable<Integer> cir) {
        int raised = SuperStackConfig.raiseVanillaDefault(cir.getReturnValue());
        ItemStack current = ((Slot) (Object) this).getItem();
        cir.setReturnValue(current.isEmpty() ? raised : Math.max(raised, current.getCount()));
    }

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    private void superstack$neverBelowContents(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        ItemStack current = ((Slot) (Object) this).getItem();
        if (current.isEmpty() || !ItemStack.isSameItemSameTags(current, stack)) {
            return;
        }
        cir.setReturnValue(Math.max(cir.getReturnValue(), current.getCount()));
    }
}
