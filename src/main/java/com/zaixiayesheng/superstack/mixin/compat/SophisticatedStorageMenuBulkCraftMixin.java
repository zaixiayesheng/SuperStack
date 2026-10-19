package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackSophisticatedBulkCraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts shift-clicking the crafting upgrade result slot and lets SuperStack craft the whole
 * available batch in one pass. When SuperStack cannot handle the click (container recipes, missing
 * internals) the original loop runs unchanged.
 */
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase", remap = false)
public class SophisticatedStorageMenuBulkCraftMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void superstack$bulkCraft(int slotId, int mouseButton, ClickType clickType, Player player, CallbackInfo ci) {
        if (clickType != ClickType.QUICK_MOVE) {
            return;
        }
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (slotId < 0 || slotId >= menu.slots.size()) {
            return;
        }
        Slot slot = menu.slots.get(slotId);
        if (!(slot instanceof ResultSlot)) {
            return;
        }
        if (SuperStackSophisticatedBulkCraft.tryCraftAll(menu, slot, player)) {
            ci.cancel();
        }
    }
}