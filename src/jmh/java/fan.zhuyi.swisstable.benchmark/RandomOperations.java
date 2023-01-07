package fan.zhuyi.swisstable.benchmark;

import fan.zhuyi.swisstable.GenericSwissTable;
import fan.zhuyi.swisstable.SwissTable;
import org.openjdk.jmh.annotations.*;

import java.util.HashMap;
import java.util.Random;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Fork(value = 1)
@Warmup(iterations = 5, time = 5)
public class RandomOperations {
    private static final Random GENERATOR = new Random();
    private static final int ROUNDS = 100000;
    private Operation[] operations;

    private sealed interface Operation {
        long operand();
        record Insert(long operand) implements Operation {}
        record Delete(long operand) implements Operation {}
        record Find(long operand) implements Operation {}
        static Operation generate() {
            var data = GENERATOR.nextInt() % 3;
            if (data == 0) return new Insert(GENERATOR.nextLong());
            if (data == 1) return new Delete(GENERATOR.nextLong());
            return new Find(GENERATOR.nextLong());
        }
    }

    @Setup
    public void initOperations() {
        operations = new Operation[ROUNDS];
        for (int i = 0; i < operations.length; ++i) {
            operations[i] = Operation.generate();
        }
    }

    @Benchmark
    public HashMap<Long, Long> hashMapRandomOperation() {
        var map = new HashMap<Long, Long>();
        for (var i : operations) {
            switch (i) {
                case Operation.Insert insert -> map.put(insert.operand(), insert.operand());
                case Operation.Delete delete -> map.remove(delete.operand());
                case Operation.Find find -> map.get(find.operand());
            }
        }
        return map;
    }

    @Benchmark
    public SwissTable<Long, Long> swissTableRandomOperation() {
        var map = new SwissTable<Long, Long>(x -> x);
        for (var i : operations) {
            switch (i) {
                case Operation.Insert insert -> map.insert(insert.operand(), insert.operand());
                case Operation.Delete delete -> map.erase(delete.operand());
                case Operation.Find find -> map.find(find.operand());
            }
        }
        return map;
    }

    @Benchmark
    public GenericSwissTable<Long, Long> genericSwissTableRandomOperation() {
        var map = new GenericSwissTable<Long, Long>(x -> x);
        for (var i : operations) {
            switch (i) {
                case Operation.Insert insert -> map.insert(insert.operand(), insert.operand());
                case Operation.Delete delete -> map.erase(delete.operand());
                case Operation.Find find -> map.find(find.operand());
            }
        }
        return map;
    }
}
