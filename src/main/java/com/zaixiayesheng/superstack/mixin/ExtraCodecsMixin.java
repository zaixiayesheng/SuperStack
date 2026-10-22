package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 原版把堆叠数量限制在 1~99（{@code ExtraCodecs.intRange(1, 99)}），整个原版只有两处用它：
 * {@code DataComponents.MAX_STACK_SIZE} 与 {@code ItemStack.CODEC} 的 count 字段。
 *
 * <p>这里把它换成配置里的堆叠上限：{@code SuperStackConfig.codecUpperBound()} 会优先用已加载的配置，
 * 配置还没读完时直接读一遍配置文件，所以「改配置 + 重启」就生效，不需要额外的手动步骤。</p>
 */
@Mixin(ExtraCodecs.class)
public abstract class ExtraCodecsMixin {
    @ModifyVariable(method = "intRange", at = @At("HEAD"), argsOnly = true, index = 1)
    private static int superstack$raiseUpperBound(int bound) {
        return SuperStackConfig.raiseCodecUpperBound(bound);
    }
}
