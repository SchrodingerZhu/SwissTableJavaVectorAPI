package fan.zhuyi.swisstable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WyHashTest {
    @Test
    public void sanityTest() {
        String[] cases = {"", "a", "abc", "message digest", "abcdefghijklmnopqrstuvwxyz", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789", "12345678901234567890123456789012345678901234567890123456789012345678901234567890"};
        long[] hash = {0x0409638ee2bde459L, 0xa8412d091b5fe0a9L, 0x32dd92e4b2915153L, 0x8619124089a3a16bL, 0x7a43afb61d7f5f40L, 0xff42329b90e50d58L, 0xc39cab13b115aad3L};
        for (int i = 0; i < cases.length; ++i) {
            var hasher = WyHash.create(i).asStringHasher();
            assertEquals(hash[i], hasher.hash(cases[i]));
        }
    }


}
