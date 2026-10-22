package com.zaixiayesheng.superstack;

import java.util.List;
import net.minecraft.core.NonNullList;
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
 * 一格三万多物品就意味着同一个 tick 里跑三万多轮，客户端卡死、联机时服务端 tick 超时。
 *
 * <p>这里换成批量：先算出材料最多够合几次、背包最多还能放几次，取较小值，然后一次扣材料、
 * 一次把产物按堆叠上限分配进背包。整个过程只扫一遍槽位，与堆叠大小无关。
 *
 * <p>堆叠上限允许开到 int 上限，所以所有「数量 × 次数」的中间量都用 long 计算，
 * 产物按物品自身的上限分块插入，任何一步都不会因为 int 溢出而丢掉物品。
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

        // 记录配方会返还的容器物品（桶、瓶之类），合成后按次数补回
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(9, ItemStack.EMPTY);
        if (resultSlots instanceof RecipeCraftingHolder holder) {
            RecipeHolder<?> usedRecipe = holder.getRecipeUsed();
            if (usedRecipe != null && usedRecipe.value() instanceof CraftingRecipe craftingRecipe) {
                CraftingInput input = craftSlots.asCraftInput();
                NonNullList<ItemStack> computed = craftingRecipe.getRemainingItems(input);
                for (int i = 0; i < Math.min(computed.size(), remainingItems.size()); i++) {
                    remainingItems.set(i, computed.get(i));
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

        int perCraft = Math.max(1, result.getCount());
        long freeSpace = countFreeSpace(menu, result, inventoryStart, inventoryEnd);
        int crafts = BulkCraftMath.capBySpace(limitedByInput, freeSpace, perCraft);
        if (crafts <= 0) {
            return false;
        }

        // 一次扣掉材料
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            if (!craftSlots.getItem(i).isEmpty()) {
                craftSlots.removeItem(i, crafts);
            }
        }

        // 把配方返还的容器物品按次数补回原格（放不下的进背包，再放不下就掉出来）
        for (int i = 0; i < remainingItems.size() && i < craftSlots.getContainerSize(); i++) {
            ItemStack rem = remainingItems.get(i);
            if (rem.isEmpty()) {
                continue;
            }
            long total = (long) rem.getCount() * crafts;
            while (total > 0) {
                int chunk = BulkCraftMath.nextChunk(total, rem.getMaxStackSize());
                ItemStack gave = rem.copyWithCount(chunk);
                ItemStack current = craftSlots.getItem(i);
                if (current.isEmpty()) {
                    craftSlots.setItem(i, gave);
                } else if (ItemStack.isSameItemSameComponents(current, gave)) {
                    int room = Math.max(0, current.getMaxStackSize() - current.getCount());
                    if (room >= chunk) {
                        current.grow(chunk);
                    } else {
                        current.grow(room);
                        gave.shrink(room);
                        if (!player.getInventory().add(gave)) {
                            player.drop(gave, false);
                        }
                    }
                } else if (!player.getInventory().add(gave)) {
                    player.drop(gave, false);
                }
                total -= chunk;
            }
        }

        // 一次把产物分配进背包，按物品自身上限分块
        long remainingOutput = (long) crafts * perCraft;
        long produced = remainingOutput;
        while (remainingOutput > 0) {
            int chunk = BulkCraftMath.nextChunk(remainingOutput, result.getMaxStackSize());
            long leftover = insert(menu, result, chunk, inventoryStart, inventoryEnd);
            if (leftover > 0) {
                ItemStack drop = result.copyWithCount((int) Math.min(leftover, result.getMaxStackSize()));
                if (!player.getInventory().add(drop)) {
                    player.drop(drop, false);
                }
            }
            remainingOutput -= chunk;
        }

        int producedInt = (int) Math.min(produced, Integer.MAX_VALUE);
        result.onCraftedBy(player.level(), player, producedInt);
        player.awardStat(Stats.ITEM_CRAFTED.get(result.getItem()), crafts);
        if (resultSlots instanceof RecipeCraftingHolder holder) {
            holder.awardUsedRecipes(player, craftSlots.getItems());
        }
        EventHooks.firePlayerCraftingEvent(player, result, craftSlots);

        // 让菜单按剩下的材料重新算结果格
        menu.slotsChanged(craftSlots);
        return true;
    }

    /** 背包里还能放下多少个这种物品（含空格），用 long 累加，上限开满 int 也不会溢出。 */
    private static long countFreeSpace(AbstractContainerMenu menu, ItemStack result, int start, int end) {
        long space = 0L;
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
    private static long insert(AbstractContainerMenu menu, ItemStack result, int count, int start, int end) {
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
