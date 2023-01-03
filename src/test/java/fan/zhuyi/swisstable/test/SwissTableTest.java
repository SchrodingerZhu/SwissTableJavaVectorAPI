package fan.zhuyi.swisstable.test;

import fan.zhuyi.swisstable.SwissTable;
import fan.zhuyi.swisstable.WyHash;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


public class SwissTableTest {
    @Test
    public void simpleTest() {
        SwissTable<Integer, Integer> table = new SwissTable<>(WyHash.DEFAULT.asIntegerHasher());
        for (int i = 0; i < 1000; ++i) {
            table.insert(i , i);
        }
        for (int i = 0; i < 1000; ++i) {
            var entry = table.findEntry(i);
            assertTrue(entry.value().isPresent());
            assertEquals(entry.value().get(), i);
        }
        for (int i = 0; i < 1000; ++i) {
            var value = table.erase(i);
            assertTrue(value.isPresent());
            assertEquals(value.get(), i);

            for (int j = 0; j < i; ++j) {
                assertFalse(table.find(j).isPresent());
            }
            for (int j = i + 1; j < 1000; ++j) {
                assertTrue(table.find(j).isPresent());
            }
        }

        for (int i = 2000; i >= 0; --i) {
            table.insert(i , i);
        }
        for (int i = 0; i <= 2000; ++i) {
            var entry = table.findEntry(i);
            assertTrue(entry.value().isPresent());
            assertEquals(entry.value().get(), i);
        }
    }
}
