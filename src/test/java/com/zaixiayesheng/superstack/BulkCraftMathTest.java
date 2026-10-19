package com.zaixiayesheng.superstack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BulkCraftMathTest {

    private static final int CAP = 32768;

    /** Diamonds -> blocks: 9 diamonds per block, one block per craft. */
    private static long[] craftAll(long grid, long storage, long inventory, int slotsUsing, int perCraft,
        long freeSpace) {
        long available = BulkCraftMath.available(grid, storage, inventory);
        int crafts = BulkCraftMath.crafts(available, slotsUsing);
        crafts = BulkCraftMath.capBySpace(crafts, freeSpace, perCraft);
        long consumed = BulkCraftMath.expectedConsumed(crafts, slotsUsing);
        long produced = BulkCraftMath.expectedProduced(crafts, perCraft);
        assertEquals(available - consumed >= 0, true, "cannot consume more than available");
        assertTrue(BulkCraftMath.balanced(consumed, crafts, slotsUsing), "consumption must match crafts");
        return new long[] {crafts, consumed, produced};
    }

    @Test
    void craftsEverythingFromStorageInOneClick() {
        // 1,638,542 diamonds, 9 grid slots, plenty of free output space.
        long[] result = craftAll(9L * CAP, 1_638_542L - 9L * CAP, 0, 9, 1, 100_000_000L);
        assertEquals(182_060L, result[0], "1,638,540 materials pay for 182,060 crafts");
        assertEquals(1_638_540L, result[1], "consumed exactly what the crafts need");
        assertEquals(182_060L, result[2]);
        assertEquals(1_638_540L + 2L, 1_638_542L, "leftover is 2 diamonds, nothing lost");
    }

    @Test
    void neverConsumesMoreThanAvailable() {
        long[] result = craftAll(8L, 1_638_534L, 0, 9, 1, 100_000_000L);
        assertEquals(182_060L, result[0]);
        assertEquals(1_638_540L, result[1]);
        assertTrue(result[1] <= 1_638_542L);
    }

    @Test
    void outputSpaceLimitsCrafts() {
        long[] result = craftAll(9L * CAP, 1_000_000L, 0, 9, 1, 8L);
        assertEquals(8L, result[0], "only eight blocks fit");
        assertEquals(72L, result[1], "and only 8 * 9 diamonds are taken");
        assertEquals(8L, result[2]);
    }

    @Test
    void zeroSpaceMeansNoCraftAndNoConsumption() {
        long[] result = craftAll(9L * CAP, 1_000_000L, 0, 9, 1, 0L);
        assertEquals(0L, result[0]);
        assertEquals(0L, result[1]);
        assertEquals(0L, result[2]);
    }

    @Test
    void multiOutputRecipeCountsCorrectly() {
        // 6 glass -> 16 panes: perCraft = 16.
        long[] result = craftAll(6L * 100, 0, 0, 6, 16, 1_000_000L);
        assertEquals(100L, result[0]);
        assertEquals(600L, result[1]);
        assertEquals(1_600L, result[2]);
    }

    @Test
    void missingIngredientBlocksTheWholeCraft() {
        assertEquals(0, BulkCraftMath.crafts(8, 9), "eight items cannot pay for a nine-slot recipe");
        assertEquals(0, BulkCraftMath.crafts(1_000, 0), "no slots means no crafts");
    }

    @Test
    void chunksCoverTheWholeOutputWithoutTruncation() {
        long produced = 182_060L;
        List<Integer> chunks = new ArrayList<>();
        long remaining = produced;
        while (remaining > 0) {
            int chunk = BulkCraftMath.nextChunk(remaining, CAP);
            chunks.add(chunk);
            remaining -= chunk;
        }
        assertEquals((long) chunks.stream().mapToInt(Integer::intValue).sum(), produced);
        assertEquals(BulkCraftMath.chunkCount(produced, CAP), chunks.size());
        assertTrue(chunks.stream().allMatch(c -> c > 0 && c <= CAP), "every chunk fits in one stack");
        assertEquals(6, chunks.size(), "182,060 blocks need six stacks of 32,768");
    }

    @Test
    void balanceDetectsLeaksAndDuplicates() {
        assertTrue(BulkCraftMath.balanced(72, 8, 9));
        assertFalse(BulkCraftMath.balanced(71, 8, 9), "one item leaked");
        assertFalse(BulkCraftMath.balanced(73, 8, 9), "one item duplicated");
        assertTrue(BulkCraftMath.balanced(0, 0, 9), "nothing crafted, nothing consumed");
    }
}
