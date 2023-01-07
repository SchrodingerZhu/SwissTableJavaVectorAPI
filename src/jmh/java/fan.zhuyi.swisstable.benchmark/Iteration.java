package fan.zhuyi.swisstable.benchmark;

import fan.zhuyi.swisstable.GenericSwissTable;
import fan.zhuyi.swisstable.SwissTable;
import org.openjdk.jmh.annotations.*;

import java.util.HashMap;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Fork(value = 1)
@Warmup(iterations = 5, time = 5)
public class Iteration extends BenchmarkBase {

    SwissTable<Long, Long> swissTable;
    GenericSwissTable<Long, Long> genericSwissTable;
    HashMap<Long, Long> hashMap;

    @Setup
    public void init() {
        swissTable = new SwissTable<>(x -> x);
        genericSwissTable = new GenericSwissTable<>(x -> x);
        hashMap = new HashMap<>();
        for (var i : values) {
            swissTable.insert(i, i);
            genericSwissTable.insert(i, i);
            hashMap.put(i, i);
        }
    }

    @Benchmark
    public long genericSwissTableIteration() {
        long res = 0;
        for (var i : genericSwissTable) {
            res += i.value();
        }
        return res;
    }

    @Benchmark
    public long swissTableIteration() {
        long res = 0;
        for (var i : swissTable) {
            res += i.value();
        }
        return res;
    }

    @Benchmark
    public long hashMapIteration() {
        long res = 0;
        for (var i : hashMap.entrySet()) {
            res += i.getValue();
        }
        return res;
    }
}
