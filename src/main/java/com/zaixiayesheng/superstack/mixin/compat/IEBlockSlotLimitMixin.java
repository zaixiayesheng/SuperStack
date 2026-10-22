package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
        "blusunrize.immersiveengineering.common.blocks.wooden.WoodenCrateBlockEntity",
        "blusunrize.immersiveengineering.common.blocks.wooden.SorterBlockEntity$SorterInventoryHandler",
        "blusunrize.immersiveengineering.common.blocks.wooden.ItemBatcherBlockEntity",
        "blusunrize.immersiveengineering.common.blocks.metal.ToolboxBlockEntity",
        "blusunrize.immersiveengineering.common.blocks.metal.ChuteBlockEntity$ChuteInventoryHandler",
        "blusunrize.immersiveengineering.common.blocks.metal.ConveyorBeltBlockEntity$ConveyorInventoryHandler",
        "blusunrize.immersiveengineering.common.blocks.multiblocks.logic.ShelfLogic$ShelfItemHandler",
        "blusunrize.immersiveengineering.common.util.inventory.InsertOnlyInventory"
}, remap = false)
public class IEBlockSlotLimitMixin {

    @Inject(method = "getSlotLimit(I)I", at = @At("RETURN"), cancellable = true)
    private void superstack$raiseSlotLimit(int slot, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SuperStackConfig.raiseCompatLimit(SuperStackConfig.CompatPatch.IMMERSIVE_ENGINEERING, cir.getReturnValue()));
    }
}
