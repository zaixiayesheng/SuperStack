package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 搴旂敤鑳芥簮 2锛圓E2锛夌粰 Forge 鐢ㄧ殑搴撳瓨閫傞厤鍣ㄣ€? *
 * <p>{@code InternalInventoryItemHandler} 鎶?{@code getSlotLimit} 鐩存帴濮旀墭缁欏唴閮ㄥ簱瀛橈紝
 * 鍐呴儴搴撳瓨閭ｈ竟宸茬粡琛ヨ繃锛岃繖閲屽啀鍏滀竴灞傦細涓囦竴鏌愪釜瀹炵幇鑷繁杩斿洖 64/99锛屼篃鑳芥姮璧锋潵銆? */
@Mixin(targets = "appeng.api.inventories.InternalInventoryItemHandler", remap = false)
public abstract class Ae2InternalInventoryItemHandlerMixin {
    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true)
    private void superstack$raiseLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.raiseCompatLimit(SuperStackConfig.CompatPatch.AE2, cir.getReturnValue()));
    }
}
