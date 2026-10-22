package com.zaixiayesheng.superstack.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 原版 {@link Container#getMaxStackSize()} 默认返回 99，箱子 / 潜影盒 / 背包里没重写它的容器都吃这个值；
 * {@code getMaxStackSize(ItemStack)} 则是 {@code min(容器上限, 物品上限)}。
 *
 * <p>两件事：一是把 99 换成配置上限；二是让返回值永远不小于传入堆叠的数量。
 * 后者很关键——{@code SimpleContainer#setItem}、{@code BaseContainerBlockEntity} 收物品时会调
 * {@code stack.limitSize(getMaxStackSize(stack))}，上限若小于数量会把堆叠截断（看起来就是「放进去少了一大截」）。
 */
@Mixin(Container.class)
public interface ContainerMixin {
    @ModifyReturnValue(method = "getMaxStackSize()I", at = @At("RETURN"))
    private int superstack$raiseVanillaLimit(int original) {
        return SuperStackConfig.raiseVanillaDefault(original);
    }

    @ModifyReturnValue(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"))
    private int superstack$neverBelowStackCount(int original, ItemStack stack) {
        return stack.isEmpty() ? original : Math.max(original, stack.getCount());
    }
}
