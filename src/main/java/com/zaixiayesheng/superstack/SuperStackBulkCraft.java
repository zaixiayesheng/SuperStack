package com.zaixiayesheng.superstack;

import java.util.List;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.event.EventHooks;

/**
 * 一次点击把能合的全都合完。
 *
 * <p>原版的做法是一轮一轮地调 {@code quickMoveStack}，每轮重新查配方、扣材料、把产物塞进背包；
 * 一格 32768 个物品就意味着同一个 tick 里跑三万多次，客户端卡死、联机时服务端 tick 超时。
 *
 * <p>这里换成批量：先算出材料最多够合几次、背包最多还能放几次，取较小值，然后一次扣材料、
 * 一次把产物按堆叠上限分配进背包。整个过程只扫一遍槽位，与堆叠大小无关。
 *
 * <p>带「剩余物品」的配方（桶装牛奶之类）不在这里处理，交回原版循环，避免把容器物品算错。
 */
public final class SuperStackBulkCraft {

    private SuperStackBulkCraft() {
    }

    /**
     * 尝试把结果格里的这一批合成一次做完。
     *
     * @return true 表示已经处理完（调用方直接返回空栈结束原版循环），false 表示交回原版
     */
    public static boolean tryCraftAll(AbstractContainerMenu menu, CraftingContainer craftSlots,
                                      ResultContainer resultSlots, Player player, int inventoryStart, int inventoryEnd) {
        ItemStack result = resultSlots.getItem(0);
        if (result.isEmpty() || result.getCount() <= 0) {
            return false;
        }

        // 配方会返还容器物品时不批量处理
        if (resultSlots instanceof RecipeCraftingHolder holder) {
            RecipeHolder<?> usedRecipe = holder.getRecipeUsed();
            if (usedRecipe != null && usedRecipe.value() instanceof CraftingRecipe craftingRecipe) {
                CraftingInput input = craftSlots.asCraftInput();
                List<ItemStack> remaining = craftingRecipe.getRemainingItems(input);
                for (ItemStack stack : remaining) {
                    if (!stack.isEmpty()) {
                        return false;
                    }
                }
            }
        }

        // 材料够合几次：每个非空格子一次合成各扣 1 个
        int limitedByInput = Integer.MAX_VALUE;
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (!stack.isEmpty()) {
                limitedByInput = Math.min(limitedByInput, stack.getCount());
            }
        }
        if (limitedByInput == Integer.MAX_VALUE) {
            return false;
        }

        int perCraft = result.getCount();
        int limitedBySpace = countFreeSpace(menu, result, inventoryStart, inventoryEnd) / perCraft;
        int crafts = Math.min(limitedByInput, limitedBySpace);
        if (crafts <= 0) {
            return false;
        }

        // 一次扣掉材料
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            if (!craftSlots.getItem(i).isEmpty()) {
                craftSlots.removeItem(i, crafts);
            }
        }

        // 一次把产物分配进背包
        int produced = crafts * perCraft;
        int leftover = insert(menu, result, produced, inventoryStart, inventoryEnd);
        if (leftover > 0) {
            ItemStack drop = result.copyWithCount(leftover);
            if (!player.getInventory().add(drop)) {
                player.drop(drop, false);
            }
        }

        result.onCraftedBy(player.level(), player, produced);
        player.awardStat(Stats.ITEM_CRAFTED.get(result.getItem()), crafts);
        if (resultSlots instanceof RecipeCraftingHolder holder) {
            holder.awardUsedRecipes(player, craftSlots.getItems());
        }
        EventHooks.firePlayerCraftingEvent(player, result, craftSlots);

        // 让菜单按剩下的材料重新算结果格
        menu.slotsChanged(craftSlots);
        return true;
    }

    /** 背包里还能放下多少个这种物品（含空格）。 */
    private static int countFreeSpace(AbstractContainerMenu menu, ItemStack result, int start, int end) {
        int space = 0;
        for (int i = start; i < end; i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            int limit = slot.getMaxStackSize(result);
            if (stack.isEmpty()) {
                space += limit;
            } else if (ItemStack.isSameItemSameComponents(stack, result)) {
                space += Math.max(0, limit - stack.getCount());
            }
        }
        return space;
    }

    /** 把 count 个 result 塞进背包，返回没塞下的数量。 */
    private static int insert(AbstractContainerMenu menu, ItemStack result, int count, int start, int end) {
        int remaining = count;
        for (int i = start; i < end && remaining > 0; i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            int limit = slot.getMaxStackSize(result);
            if (stack.isEmpty()) {
                int amount = Math.min(remaining, limit);
                slot.set(result.copyWithCount(amount));
                slot.setChanged();
                remaining -= amount;
            } else if (ItemStack.isSameItemSameComponents(stack, result)) {
                int amount = Math.min(remaining, limit - stack.getCount());
                if (amount > 0) {
                    stack.grow(amount);
                    slot.setChanged();
                    remaining -= amount;
                }
            }
        }
        return remaining;
    }
}
