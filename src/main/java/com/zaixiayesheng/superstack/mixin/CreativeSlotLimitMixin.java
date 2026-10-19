package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 创造模式放进背包/丢到地上的数量上限。
 *
 * <p>{@code ServerGamePacketListenerImpl.handleSetCreativeModeSlot} 里写死了判断
 * {@code itemstack.getCount() <= 64}，超过 64 的创造物品包会被直接丢掉：
 * 客户端本地显示 32768，服务端却当作非法包忽略，于是「拿出来的东西一开箱子就没了、
 * 丢到地上闪一下就消失」。
 *
 * <p>把 64 换成配置的堆叠上限，创造拿出来的大堆叠才会真正落到服务端。
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CreativeSlotLimitMixin {
    @ModifyConstant(
            method = "handleSetCreativeModeSlot",
            constant = @Constant(intValue = 64),
            require = 0
    )
    private int superstack$raiseCreativeSlotLimit(int original) {
        return SuperStackConfig.maxStackSize();
    }
}