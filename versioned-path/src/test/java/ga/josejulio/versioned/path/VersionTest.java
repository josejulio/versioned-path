package ga.josejulio.versioned.path;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VersionTest {

    @Test
    public void validVersionStringTest()  {
        Version v = new Version("1.0.0");
        assertEquals(1, v.getMajor());
        assertEquals(0, v.getMinor());
        assertEquals(0, v.getPatch());

        v = new Version("1.2.0");
        assertEquals(1, v.getMajor());
        assertEquals(2, v.getMinor());
        assertEquals(0, v.getPatch());

        v = new Version("1.2.3");
        assertEquals(1, v.getMajor());
        assertEquals(2, v.getMinor());
        assertEquals(3, v.getPatch());

        v = new Version("4.8.7");
        assertEquals(4, v.getMajor());
        assertEquals(8, v.getMinor());
        assertEquals(7, v.getPatch());

        v = new Version("1");
        assertEquals(1, v.getMajor());
        assertEquals(0, v.getMinor());
        assertEquals(0, v.getPatch());

        v = new Version("1.2");
        assertEquals(1, v.getMajor());
        assertEquals(2, v.getMinor());
        assertEquals(0, v.getPatch());
    }

    @Test
    public void invalidVersionStringTest() {
        // With negative numbers
        assertThrows(RuntimeException.class, () -> new Version("-1.2.3"));
        assertThrows(RuntimeException.class, () -> new Version("1.-2.3"));
        assertThrows(RuntimeException.class, () -> new Version("1.2.-3"));
        assertThrows(RuntimeException.class, () -> new Version("-1.-2.-3"));

        // other than numbers
        assertThrows(RuntimeException.class, () -> new Version("a.b.c"));
        assertThrows(RuntimeException.class, () -> new Version("1.b.c"));
        assertThrows(RuntimeException.class, () -> new Version("a.2.c"));
        assertThrows(RuntimeException.class, () -> new Version("a.b.3"));
    }

    @Test
    public void toStringTest() {
        assertEquals(
                "1.0.0",
                new Version("1.0.0").toString()
        );

        assertEquals(
                "1.2.0",
                new Version("1.2.0").toString()
        );

        assertEquals(
                "1.2.3",
                new Version("1.2.3").toString()
        );
    }

    @Test
    public void toShortVersionStringTest() {
        assertEquals(
                "1",
                new Version("1.0.0").toShortVersionString()
        );

        assertEquals(
                "1.2",
                new Version("1.2.0").toShortVersionString()
        );

        assertEquals(
                "1.2.3",
                new Version("1.2.3").toShortVersionString()
        );
    }

    @Test
    public void compareSortTest() {
        Version v1 = new Version(1, 0, 0);
        Version v2 = new Version(2, 0, 0);
        Version v1_0_1 = new Version(1, 0, 1);
        Version v1_1 = new Version(1, 1, 0);
        Version v1_1_1 = new Version(1, 1, 1);
        Version v3_0_1 = new Version(3, 0, 1);

        assertEquals(
                List.of(v1, v1_0_1, v1_1, v1_1_1, v2, v3_0_1),
                List.of(v3_0_1, v1_1, v1, v2, v1_0_1, v1_1_1).stream().sorted().collect(Collectors.toList())
        );
    }

    @Test
    public void compareTest() {
        Version v1 = new Version(1, 0, 0);
        Version v2 = new Version(2, 0, 0);

        Version v1_1 = new Version(1, 1, 0);
        Version v1_2 = new Version(1, 2, 0);

        Version v1_1_1 = new Version(1, 1, 1);
        Version v1_1_5 = new Version(1, 1, 5);

        symmetryCompareTest(v1, v1_1);
        symmetryCompareTest(v1, v1_1_1);
        symmetryCompareTest(v1, v1_1_5);
        symmetryCompareTest(v1, v1_2);
        symmetryCompareTest(v1, v2);

        symmetryCompareTest(v1_1, v1_1_1);
        symmetryCompareTest(v1_1, v1_1_5);
        symmetryCompareTest(v1_1, v1_2);
        symmetryCompareTest(v1_1, v2);

        symmetryCompareTest(v1_1_1, v1_1_5);
        symmetryCompareTest(v1_1_1, v1_2);
        symmetryCompareTest(v1_1_1, v2);

        symmetryCompareTest(v1_1_5, v1_2);
        symmetryCompareTest(v1_1_5, v2);

        symmetryCompareTest(v1_2, v2);
    }

    @Test
    public void equalTest() {
        assertEquals(
                new Version(1, 2, 3),
                new Version("1.2.3")
        );

        assertEquals(
                new Version(4, 0, 1),
                new Version("4.0.1")
        );

        assertEquals(
                new Version(9, 0, 0),
                new Version("9.0.0")
        );

        assertNotEquals(
                new Version(1, 0, 0),
                new Version(1, 0, 1)
        );

        assertNotEquals(
                new Version(1, 0, 0),
                new Version(1, 5, 0)
        );
    }

    void symmetryCompareTest(Version shouldBeLess, Version shouldBeGreat) {
        assertEquals(
                -1,
                shouldBeLess.compareTo(shouldBeGreat)
        );

        assertEquals(
                1,
                shouldBeGreat.compareTo(shouldBeLess)
        );

        assertEquals(
                0,
                shouldBeGreat.compareTo(shouldBeGreat)
        );

        assertEquals(
                0,
                shouldBeLess.compareTo(shouldBeLess)
        );
    }
}
