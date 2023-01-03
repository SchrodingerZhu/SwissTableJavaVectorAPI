package fan.zhuyi.swisstable;

import org.junit.jupiter.api.Test;


public class SwissTableTest {
    @Test
    public void simpleTest() {
        SwissTable<Integer, Integer> table = new SwissTable<>(WyHash.DEFAULT.asIntegerHasher());
        for (int i = 0; i < 1000; ++i) {
            table.insert(i , i);
        }
    }

    @Test
    public void randomWalk() {

    }
}
