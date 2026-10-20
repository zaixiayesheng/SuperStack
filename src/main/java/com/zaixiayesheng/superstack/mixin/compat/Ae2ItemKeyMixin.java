package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 应用能源的 {@code AEItemKey} 在创建时把物品当时的堆叠上限缓存成 int。
 * 如果这个 key 是在本模组抬高上限之前创建，之后从 AE 终端取出、以及存储里按堆叠拆分的数量都会卡在 64。
 *
 * <p>这里把 getter 的返回值抬到配置的堆叠上限：存储里的长整型数量不变，只是允许一次取出更大的堆叠。
 */
@Mixin(targets = "appeng.api.stacks.AEItemKey", remap = false)
public abstract class Ae2ItemKeyMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true, remap = false)
    private void superstack$raiseMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        int configured = SuperStackConfig.compatEnabled(SuperStackConfig.CompatPatch.AE2)
                ? SuperStackConfig.maxStackSize() : SuperStackConfig.VANILLA_MAX_STACK_SIZE;
        if (cir.getReturnValue() < configured) {
            cir.setReturnValue(configured);
        }
    }
}
