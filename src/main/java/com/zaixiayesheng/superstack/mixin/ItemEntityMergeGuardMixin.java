package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让地上掉落的物品按配置的堆叠上限合并。
 *
 * <p>原版两处都写死了 64：{@code areMergable} 用 int 相加判断「两个堆叠加起来放不放得下」，
 * {@code tryToMerge} 每次只按 64 搬运。堆叠上限提到 32768 甚至 32 位整数上限后：
 * <ul>
 *   <li>两个大堆叠相加会在 int 里溢出成负数，直接判成「放得下」，合并逻辑把数量算错；</li>
 *   <li>即使判对了，也永远只合并到 64，超出部分留在另一个实体里，看起来像「地上掉的东西不合并」。</li>
 * </ul>
 * 这里把 64 换成配置值，并用 long 相加做溢出保护：真正的上限判断仍交给原版。
 */
@Mixin(ItemEntity.class)
public class ItemEntityMergeGuardMixin {

    @Inject(method = "areMergable(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void superstack$guardMerge(ItemStack destination, ItemStack origin,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (destination.isEmpty() || origin.isEmpty()) {
            return;
        }
        long sum = (long) destination.getCount() + origin.getCount();

        if (sum > origin.getMaxStackSize()) {
            cir.setReturnValue(false);
        }
    }

    @ModifyConstant(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",
            constant = @Constant(intValue = 64), require = 0)
    private static int superstack$mergeAmount(int original) {
        return SuperStackConfig.maxStackSize();
    }
}
