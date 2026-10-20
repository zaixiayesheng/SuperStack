package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * 修复历史存档里被写成「显式 max_stack_size」的物品。
 *
 * <p>1.0.6 早期的测试版在应用能源的键里写死过上限，也出现过「把组件删掉」的写法：删除会在组件补丁里
 * 留下一个「已移除」标记，把物品默认值一起盖掉，读取时上限变成 1。结果是同一物品在存档里出现几种
 * 互不相等的身影（32768 / 64 / 1），彼此不能合并，其中一种还只能一个个取。
 *
 * <p>这里不改存档，只在读取时纠正行为：
 * <ul>
 *   <li>上限：只要这份堆叠带着 max_stack_size 补丁（无论是显式值还是「已移除」标记），且解析出来的
 *       上限不超过原版 64，就按配置值返回。</li>
 *   <li>身份：比较两份堆叠是否同一种物品时，先把这类补丁归一回物品默认值再比，让历史遗留的几种
 *       身影重新算作同一种，能正常合并、也能在应用能源里并成一个键。</li>
 * </ul>
 * 对没有这类补丁的普通堆叠（绝大多数）走原版逻辑，不做任何额外处理。
 */
@Mixin(ItemStack.class)
public abstract class ItemStackComponentNormalizeMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void superstack$repairStaleLimit(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValue();
        ItemStack self = (ItemStack) (Object) this;

        if (original > SuperStackConfig.VANILLA_MAX_STACK_SIZE) {
            return;
        }
        if (self.isEmpty() || !superstack$hasMaxStackSizePatch(self)) {
            return;
        }

        cir.setReturnValue(SuperStackConfig.maxStackSize());
    }

    @Inject(method = "isSameItemSameComponents", at = @At("HEAD"), cancellable = true)
    private static void superstack$compareNormalized(ItemStack stack, ItemStack other, CallbackInfoReturnable<Boolean> cir) {
        if (!stack.is(other.getItem())) {
            cir.setReturnValue(false);
            return;
        }
        if (stack.isEmpty() || other.isEmpty()) {
            return;
        }
        if (!superstack$hasMaxStackSizePatch(stack) && !superstack$hasMaxStackSizePatch(other)) {

            return;
        }
        ItemStack a = superstack$normalized(stack);
        ItemStack b = superstack$normalized(other);
        // 归一后两份都没有该补丁，再次进入本方法会直接走原版逻辑。
        cir.setReturnValue(ItemStack.isSameItemSameComponents(a, b));
    }

    private static boolean superstack$hasMaxStackSizePatch(ItemStack stack) {
        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.isEmpty()) {
            return false;
        }
        for (Map.Entry<DataComponentType<?>, ?> entry : patch.entrySet()) {
            if (entry.getKey() == DataComponents.MAX_STACK_SIZE) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack superstack$normalized(ItemStack stack) {
        ItemStack copy = stack.copy();
        // 写回「与物品默认值相同」的值：组件补丁层会直接把这条补丁删掉，
        // 显式值和「已移除」标记都会随之消失。
        copy.set(DataComponents.MAX_STACK_SIZE, copy.getItem().getDefaultMaxStackSize());
        return copy;
    }
}
