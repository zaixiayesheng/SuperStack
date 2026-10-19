package com.zaixiayesheng.superstack;

/**
 * Pure arithmetic behind the bulk crafting paths (no Minecraft types, so it can be unit tested).
 *
 * <p>The in-game code counts what is available, asks this class how many crafts that pays for,
 * then consumes exactly {@code crafts * occupiedSlots} items and produces
 * {@code crafts * perCraft} items. Everything that could silently create or destroy items
 * (rounding, overflow, per-stack truncation) lives here and is covered by tests.
 */
public final class BulkCraftMath {

    private BulkCraftMath() {
    }

    /** Total items of one ingredient the container can pay with. */
    public static long available(long grid, long storage, long inventory) {
        return Math.max(0, grid) + Math.max(0, storage) + Math.max(0, inventory);
    }

    /** How many crafts an ingredient pays for when it occupies {@code slotsUsing} grid slots. */
    public static int crafts(long available, int slotsUsing) {
        if (slotsUsing <= 0) {
            return 0;
        }
        long value = available / slotsUsing;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }

    /** Caps the craft count by the free output space. */
    public static int capBySpace(int crafts, long freeSpace, int perCraft) {
        if (perCraft <= 0 || freeSpace <= 0) {
            return 0;
        }
        return Math.min(crafts, (int) Math.min(Integer.MAX_VALUE, freeSpace / perCraft));
    }

    /** Number of input items a balanced craft run must consume. */
    public static long expectedConsumed(int crafts, int occupiedSlots) {
        return (long) crafts * Math.max(0, occupiedSlots);
    }

    /** Number of output items a balanced craft run must produce (before stack truncation). */
    public static long expectedProduced(int crafts, int perCraft) {
        return (long) crafts * Math.max(0, perCraft);
    }

    /** True when the actually consumed amount matches the crafts that were paid for. */
    public static boolean balanced(long consumed, int crafts, int occupiedSlots) {
        return consumed == expectedConsumed(crafts, occupiedSlots);
    }

    /**
     * Next insertion chunk for a bulk output. A single ItemStack can never hold more than its own
     * max stack size, so anything larger has to be split or the excess is silently dropped.
     */
    public static int nextChunk(long remaining, int maxStackSize) {
        if (remaining <= 0 || maxStackSize <= 0) {
            return 0;
        }
        return (int) Math.min(remaining, maxStackSize);
    }

    /** Number of chunks needed to insert {@code total} items with the given stack size. */
    public static int chunkCount(long total, int maxStackSize) {
        if (total <= 0 || maxStackSize <= 0) {
            return 0;
        }
        return (int) ((total + maxStackSize - 1) / maxStackSize);
    }
}
