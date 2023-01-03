package fan.zhuyi.swisstable.test;

import fan.zhuyi.swisstable.Hasher;
import fan.zhuyi.swisstable.SwissTable;
import fan.zhuyi.swisstable.WyHash;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


public class SwissTableTest {
    private static void testSequential(Hasher<Integer> hasher) {
        SwissTable<Integer, Integer> table = new SwissTable<>(hasher);
        for (int i = 0; i < 3584; ++i) {
            table.insert(i , i);
        }
        for (int i = 0; i < 3584; ++i) {
            var entry = table.findEntry(i);
            assertTrue(entry.value().isPresent());
            assertEquals(entry.value().get(), i);
        }
        for (int i = 0; i < 3584; ++i) {
            var value = table.erase(i);
            assertTrue(value.isPresent());
            assertEquals(value.get(), i);

            for (int j = 0; j < i; ++j) {
                assertFalse(table.find(j).isPresent());
            }
            for (int j = i + 1; j < 3584; ++j) {
                assertTrue(table.find(j).isPresent());
            }
        }

        for (int i = 5000; i >= 0; --i) {
            table.insert(i , i);
        }
        for (int i = 0; i <= 5000; ++i) {
            var entry = table.findEntry(i);
            assertTrue(entry.value().isPresent());
            assertEquals(entry.value().get(), i);
        }
    }

    @Test
    public void identityHashDist() {
        testSequential(key -> key);
    }

    @Test
    public void wyhashDist() {
        testSequential(WyHash.DEFAULT.asIntegerHasher());
    }
}
