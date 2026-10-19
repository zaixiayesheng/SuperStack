package com.zaixiayesheng.superstack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class OverflowGuardTest {
    private record FakeGroup(String id, long count) implements OverflowGuard.Group {
        @Override
        public boolean sameItem(OverflowGuard.Group other) {
            return other instanceof FakeGroup group && group.id.equals(id);
        }
    }

    @Test
    void singleMaxStackDoesNotOverflow() {
        assertFalse(OverflowGuard.totalsExceedInt(List.of(new FakeGroup("stone", Integer.MAX_VALUE))));
    }

    @Test
    void twoMaxStacksOverflow() {
        assertTrue(OverflowGuard.totalsExceedInt(List.of(
                new FakeGroup("stone", Integer.MAX_VALUE),
                new FakeGroup("stone", Integer.MAX_VALUE))));
    }

    @Test
    void maxPlusOneOverflows() {
        assertTrue(OverflowGuard.totalsExceedInt(List.of(
                new FakeGroup("stone", Integer.MAX_VALUE),
                new FakeGroup("stone", 1))));
    }

    @Test
    void smallStacksDoNotOverflow() {
        assertFalse(OverflowGuard.totalsExceedInt(List.of(new FakeGroup("stone", 5), new FakeGroup("stone", 5))));
    }

    @Test
    void differentItemsAreCountedSeparately() {
        assertFalse(OverflowGuard.totalsExceedInt(List.of(
                new FakeGroup("stone", Integer.MAX_VALUE),
                new FakeGroup("dirt", Integer.MAX_VALUE))));
    }

    @Test
    void emptyGroupsAreIgnored() {
        assertFalse(OverflowGuard.totalsExceedInt(List.of(
                new FakeGroup("air", 0),
                new FakeGroup("stone", Integer.MAX_VALUE),
                new FakeGroup("air", 0))));
    }
}
