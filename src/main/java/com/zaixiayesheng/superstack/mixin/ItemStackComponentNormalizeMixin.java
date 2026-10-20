package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;

/**
 * 修复早期测试版在存档里留下的、被写死的堆叠上限。
 *
 * <p>只认三种由本模组早期版本留下的痕迹，其它情况一律不动：
 * <ul>
 *   <li>堆叠上带了一个<b>显式 64</b> 的 {@code max_stack_size} 补丁（早期版本把原版 64 写进了物品）；</li>
 *   <li>补丁里是<b>「已移除」标记</b>（早期版本用 {@code remove} 删组件，反而把上限盖成了 1）；</li>
 *   <li>显式值<b>正好等于</b>物品当前的默认上限（早期版本把 32768 写进了物品，与没有这条组件等价）。</li>
 * </ul>
 * 数据包或其它模组主动设成 1、16、32 这类上限不受影响。
 *
 * <p>处理方式不改存档，只在读取时纠正：
 * <ul>
 *   <li>上限：这两种痕迹存在时按配置上限返回。</li>
 *   <li>身份：比较两份堆叠是否同一种物品时，先把这类痕迹归一回物品默认值再比，让历史遗留的几种
 *       身影重新算作同一种，能正常合并、也能在应用能源里并成一个键。</li>
 * </ul>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackComponentNormalizeMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void superstack$repairStaleLimit(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (!superstack$hasStaleLimit(self)) {
            return;
        }
        cir.setReturnValue(SuperStackConfig.maxStackSize());
    }

    private static final ThreadLocal<Boolean> SUPERSTACK_COMPARING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "isSameItemSameComponents", at = @At("HEAD"), cancellable = true)
    private static void superstack$compareNormalized(ItemStack stack, ItemStack other, CallbackInfoReturnable<Boolean> cir) {
        // 归一后再比会重新进本方法，用标志位挡住，避免递归。
        if (SUPERSTACK_COMPARING.get()) {
            return;
        }
        if (!stack.is(other.getItem())) {
            cir.setReturnValue(false);
            return;
        }
        if (stack.isEmpty() || other.isEmpty()) {
            return;
        }
        if (!superstack$hasStaleLimit(stack) && !superstack$hasStaleLimit(other)) {
            return;
        }
        ItemStack a = superstack$normalized(stack);
        ItemStack b = superstack$normalized(other);
        SUPERSTACK_COMPARING.set(Boolean.TRUE);
        try {
            cir.setReturnValue(ItemStack.isSameItemSameComponents(a, b));
        } finally {
            SUPERSTACK_COMPARING.set(Boolean.FALSE);
        }
    }

    /**
     * 应用能源会用 {@code ItemStack.hashItemAndComponents} 给 {@code AEItemKey} 缓存哈希值，
     * 之后只拿这个缓存值比较。历史遗留的堆叠和干净堆叠组件不同、哈希也就不同，会被当成两个键，
     * 各自占一份数量。这里让带痕迹的堆叠按归一后的组件参与哈希，新旧数据才会落到同一个键上。
     */
    @ModifyVariable(method = "hashItemAndComponents", at = @At("HEAD"), argsOnly = true)
    private static ItemStack superstack$normalizeForHash(ItemStack stack) {
        return superstack$hasStaleLimit(stack) ? superstack$normalized(stack) : stack;
    }

    /**
     * 这份堆叠是否带着早期版本留下的痕迹：显式 64、显式写入的本模组默认值，或者「已移除」标记。
     *
     * <p>只看 {@code getComponentsPatch()}：只有真正挂在这份堆叠上的补丁才算痕迹。
     * 干净堆叠即便取了物品默认值，补丁集里也没有这一条，不会被误判。
     */
    private static boolean superstack$hasStaleLimit(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.isEmpty()) {
            return false;
        }
        for (Map.Entry<DataComponentType<?>, ?> entry : patch.entrySet()) {
            if (entry.getKey() != DataComponents.MAX_STACK_SIZE) {
                continue;
            }
            Object raw = entry.getValue();
            if (!(raw instanceof Optional<?> optional)) {
                return false;
            }
            if (optional.isEmpty()) {
                // 组件列表里没有它、补丁里却是空值：这是「已移除」标记。
                return true;
            }
            if (!(optional.get() instanceof Integer value)) {
                return false;
            }
            if (value == SuperStackConfig.VANILLA_MAX_STACK_SIZE) {
                return true;
            }
            // 显式写成本模组的默认上限：语义上等于没有这条组件。
            return value == SuperStackConfig.maxStackSize()
                    && stack.getItem().getDefaultMaxStackSize() == value;
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
