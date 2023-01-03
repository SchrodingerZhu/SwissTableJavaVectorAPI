package fan.zhuyi.swisstable.test;

import fan.zhuyi.swisstable.Hasher;
import fan.zhuyi.swisstable.SwissTable;
import fan.zhuyi.swisstable.WyHash;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
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
            assertTrue(entry.isOccupied());
            assertEquals(entry.value(), i);
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
            assertTrue(entry.isOccupied());
            assertEquals(entry.value(), i);
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

    @Test
    public void mutualIteratorTest() {
        var gen = new Random();
        SwissTable<Integer, Integer> table = new SwissTable<>(WyHash.DEFAULT.asIntegerHasher());
        HashMap<Integer, Integer> hashMap = new HashMap<>();
        for (int i = 0; i < 10000; ++i) {
            var y = gen.nextInt();
            table.insert(i, y);
            hashMap.put(i, y);
        }
        for (int i = 0; i < 10000; ++i) {
            var x = gen.nextInt();
            while (hashMap.containsKey(x)) {
                x = gen.nextInt();
            }
            var y = gen.nextInt();
            table.insert(x, y);
            hashMap.put(x, y);
        }
        for (var i : hashMap.entrySet()) {
            var element = table.find(i.getKey());
            assertTrue(element.isPresent());
            assertEquals(element.get(), i.getValue());
        }
        for (var i : table) {
            assertTrue(i.isOccupied());
            assertEquals(hashMap.get(i.key()), i.value());
        }
        for (int i = 0; i < 20000; ++i) {
            var x = gen.nextInt() % 15000;
            table.erase(x);
            hashMap.remove(x);
            assertFalse(table.containsKey(x));
        }
        for (var i : hashMap.entrySet()) {
            var element = table.find(i.getKey());
            assertTrue(element.isPresent());
            assertEquals(element.get(), i.getValue());
        }
        for (var i : table) {
            assertTrue(i.isOccupied());
            assertEquals(hashMap.get(i.key()), i.value());
        }
    }
}
