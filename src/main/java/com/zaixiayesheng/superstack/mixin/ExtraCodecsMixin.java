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
 * <p>把上界固定放开到 int 上限，而不是跟配置走：{@code ItemStack.CODEC} 是类初始化时就建好的，
 * 如果那时配置还没读完（本模组的配置要等服务器启动才生效），编解码范围会被冻在旧值上，
 * 之后合成表同步（update_recipes）遇到大数量的配方原料就会报
 * {@code Value must be within range [1;32768]: 2147483647}。</p>
 */
@Mixin(ExtraCodecs.class)
public abstract class ExtraCodecsMixin {
    @ModifyVariable(method = "intRange", at = @At("HEAD"), argsOnly = true, index = 1)
    private static int superstack$raiseUpperBound(int bound) {
        return SuperStackConfig.raiseCodecUpperBound(bound);
    }
}
