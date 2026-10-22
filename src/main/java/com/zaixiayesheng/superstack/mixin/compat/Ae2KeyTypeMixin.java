package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import appeng.api.stacks.AEKeyType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 应用能源（AE2）的键类型。
 *
 * <p>AE2 的输入/输出总线按「每次操作搬多少」算吞吐，物品类型的默认值是 1：即使插满加速卡
 * 也只有 64 次操作 × 1 个 = 64 个/tick，堆叠再大也运不动。这里把物品类型改成「一次操作搬
 * 一整个堆叠」，与容器的槽位上限保持一致。
 */
@Mixin(value = appeng.api.stacks.AEKeyType.class, remap = false)
public abstract class Ae2KeyTypeMixin {
    @Shadow
    public abstract Class<?> getKeyClass();

    @Inject(method = "getAmountPerOperation()I", at = @At("HEAD"), cancellable = true)
    private void superstack$amountPerOperation(CallbackInfoReturnable<Integer> cir) {
        Class<?> keyClass = this.getKeyClass();
        if (keyClass == null || !"AEItemKey".equals(keyClass.getSimpleName())) {
            return;
        }
        int raised = SuperStackConfig.aeAmountPerOperation(1);
        if (raised != 1) {
            cir.setReturnValue(raised);
        }
    }
}
