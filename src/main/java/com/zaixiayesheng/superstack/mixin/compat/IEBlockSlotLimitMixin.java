package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 沉浸工程：各个自带库存的方块实体把槽位上限写死成 64。
 *
 * <p>{@code IIEInventory#getSlotLimit} 是这些机器的口径，UI 槽位与
 * {@code IEInventoryHandler} 的插入路径都会读它，所以逐个抬高。</p>
 */
@Mixin(targets = {
        "blusunrize.immersiveengineering.common.blocks.wooden.WoodenCrateBlockEntity",
        "blusunrize.immersiveengineering.common.blocks.wooden.ToolboxBlockEntity",
        "blusunrize.immersiveengineering.common.blocks.wooden.ItemBatcherBlockEntity",
        "blusunrize.immersiveengineering.common.blocks.wooden.CircuitTableBlockEntity",
        "blusunrize.immersiveengineering.common.blocks.metal.TurretBlockEntity",
        "blusunrize.immersiveengineering.common.blocks.multiblocks.logic.bottling_machine.BottlingInsertionHandler"
}, remap = false)
public class IEBlockSlotLimitMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true)
    private void superstack$slotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.raiseCompatLimit(
                SuperStackConfig.CompatPatch.IMMERSIVE_ENGINEERING, cir.getReturnValue()));
    }
}
