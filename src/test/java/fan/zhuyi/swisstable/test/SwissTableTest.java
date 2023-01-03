package fan.zhuyi.swisstable.test;

import fan.zhuyi.swisstable.Hasher;
import fan.zhuyi.swisstable.SwissTable;
import fan.zhuyi.swisstable.WyHash;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


public class SwissTableTest {
    private static void testSequential(Hasher<Integer> hasher, int firstRound, int secondRound) {
        SwissTable<Integer, Integer> table = new SwissTable<>(hasher);
        for (int i = 0; i < firstRound; ++i) {
            table.insert(i , i);
        }
        for (int i = 0; i < firstRound; ++i) {
            var entry = table.findEntry(i);
            assertTrue(entry.value().isPresent());
            assertEquals(entry.value().get(), i);
        }
        for (int i = 0; i < firstRound; ++i) {
            var value = table.erase(i);
            assertTrue(value.isPresent());
            assertEquals(value.get(), i);

            for (int j = 0; j < i; ++j) {
                assertFalse(table.find(j).isPresent());
            }
            for (int j = i + 1; j < firstRound; ++j) {
                assertTrue(table.find(j).isPresent());
            }
        }

        for (int i = secondRound; i >= 0; --i) {
            table.insert(i , i);
        }
        for (int i = 0; i <= secondRound; ++i) {
            var entry = table.findEntry(i);
            assertTrue(entry.value().isPresent());
            assertEquals(entry.value().get(), i);
        }
    }

    @Test
    public void identityHashDist() {
        testSequential(key -> key, 3584, 5000);
    }

    @Test
    public void wyhashDist() {
        testSequential(WyHash.DEFAULT.asIntegerHasher(), 3584, 5000);
    }

    @Test
    public void badHashDist() {
        testSequential(ignored -> 0, 1000, 2000);
    }
}
