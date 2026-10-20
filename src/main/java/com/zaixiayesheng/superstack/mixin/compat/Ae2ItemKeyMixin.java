package com.zaixiayesheng.superstack.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 应用能源的 {@code AEItemKey} 在创建时把物品当时的堆叠上限缓存成 int。
 * 如果这个 key 是在本模组抬高上限之前创建（例如启动阶段从物品注册表构造），
 * 之后所有从 AE 终端取出、以及存储里按堆叠拆分的数量都会卡在 64。
 *
 * <p>所以这里不改字段，直接把 getter 的返回值抬到配置的堆叠上限：
 * 存储里的长整型数量不变，只是允许一次取出/拆出更大的堆叠。
 */
@Mixin(targets = "appeng.api.stacks.AEItemKey", remap = false)
public abstract class Ae2ItemKeyMixin {

    @ModifyReturnValue(method = "getMaxStackSize()I", at = @At("RETURN"))
    private int superstack$raiseMaxStackSize(int original) {
        int configured = SuperStackConfig.maxStackSize();
        return Math.max(original, configured);
    }
}
