package com.zaixiayesheng.superstack.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import com.zaixiayesheng.superstack.SuperStackLog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 应用能源的 {@code AEItemKey} 在创建时把整份 ItemStack 存下来，取货时直接 copyWithCount。
 * 如果这个 key 是本模组抬高上限之前创建的，那份 stack 里带着显式的「max_stack_size = 64」组件：
 * 取货只能 64 个，而且和正常物品组件不同，合并时客户端乐观叠成 128、服务端再拆回 64。
 *
 * <p>处理方式是<b>删掉</b>这个显式组件，让物品自身的默认值生效，而不是写一个新的组件值。
 * 这样 AE 存下来的 NBT 与普通物品完全一致（没有多余的组件补丁），卸掉本模组也不会读不出来。
 */
@Mixin(targets = "appeng.api.stacks.AEItemKey", remap = false)
public abstract class Ae2ItemKeyMixin {

    @ModifyReturnValue(method = "getMaxStackSize()I", at = @At("RETURN"))
    private int superstack$raiseMaxStackSize(int original) {
        return original == SuperStackConfig.VANILLA_MAX_STACK_SIZE ? SuperStackConfig.maxStackSize() : original;
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, require = 0)
    private static ItemStack superstack$normalizeMaxStackSize(ItemStack stack) {
        if (stack.isEmpty() || stack.getMaxStackSize() != SuperStackConfig.VANILLA_MAX_STACK_SIZE) {
            return stack;
        }
        if (!stack.has(DataComponents.MAX_STACK_SIZE)) {
            // 没有显式组件，说明拿到的就是物品默认值，不需要动。
            return stack;
        }
        ItemStack copy = stack.copy();
        // 不能直接 remove：PatchedDataComponentMap#remove 会在补丁里写一个「已移除」标记，
        // 把物品默认值也一起盖掉，getMaxStackSize 会退化成 1。写回「与默认值相同」的值，
        // set 内部会把补丁条目删掉，等于干净地回到默认值。
        copy.set(DataComponents.MAX_STACK_SIZE, copy.getItem().getDefaultMaxStackSize());
        if (SuperStackLog.isDebugEnabled()) {
            SuperStackLog.debug("AE 堆叠归一化：" + stack.getItem() + " 清除显式上限 64，改由默认值 "
                    + copy.getMaxStackSize() + " 生效");
        }
        return copy;
    }
}
