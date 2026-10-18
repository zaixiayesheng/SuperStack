package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forge 的 ItemStackHandler。
 *
 * <p>很多模组的机器槽位、精妙系列的合成升级都用它。它的默认槽位上限是 64，
 * 而且只有配置里打开 raiseModdedSlotLimits 才替换；只把「正好 64 / 99」的老默认值换掉，
 * 别的模组显式设的 1、16、1024 保持原样。
 *
 * <p>ItemStackHandler 是 Forge 的类，不在 Minecraft 映射里，所以 remap = false。
 */
@Mixin(value = ItemStackHandler.class, remap = false)
public abstract class ItemStackHandlerMixin {
    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true)
    private void superstack$raiseModdedLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.raiseLegacyDefault(cir.getReturnValue()));
    }
}
