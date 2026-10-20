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
 * 如果这个 key 是本模组抬高上限之前创建的，那份 stack 的 max_stack_size 组件仍是 64，于是：
 * 取货只能 64 个；取出来的物品和正常物品组件不同，合并时客户端乐观叠成 128、服务端再拆回 64。
 *
 * <p>这里既让 getMaxStackSize 跟随配置，也在 key 创建时把 64 的组件归一成配置值，
 * 保证 AE 取出的堆叠和普通堆叠完全一致。
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
        ItemStack copy = stack.copy();
        copy.set(DataComponents.MAX_STACK_SIZE, SuperStackConfig.maxStackSize());
        if (SuperStackLog.isDebugEnabled()) {
            SuperStackLog.debug("AE 堆叠归一化：" + stack.getItem() + " 64 → " + SuperStackConfig.maxStackSize());
        }
        return copy;
    }
}
