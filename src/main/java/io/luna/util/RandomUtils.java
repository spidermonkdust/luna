package io.luna.util;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A static-utility class that provides additional functionality for generating pseudo-random numbers. All functions
 * in this class are backed by {@link ThreadLocalRandom} rather than the more commonly used {@link Random}. It is
 * generally preferred to use this over {@code Random} because although {@code Random} is thread safe; the same seed
 * is shared concurrently, which leads to contention between multiple threads and overhead as a result. Surprisingly
 * because of the way that {@code ThreadLocalRandom} works, even in completely single-threaded situations it runs up
 * to three times faster than {@code Random}.
 *
 * @author lare96 <http://github.com/lare96>
 * @see <a href= "http://java-performance.info/java-util-random-java-util-concurrent-threadlocalrandom-multithreaded-environments/"
 * >java.util.Random and java.util.concurrent.ThreadLocalRandom in multithreaded environments</a>
 */
public final class RandomUtils {

    /**
     * Returns a pseudo-random {@code int} value between inclusive {@code min} and inclusive {@code max}.
     *
     * @throws IllegalArgumentException If {@code max - min + 1} is less than {@code 0}.
     */
    public static int inclusive(int min, int max) {
        checkArgument(max >= min, "max < min");
        return ThreadLocalRandom.current().nextInt((max - min) + 1) + min;
    }

    /**
     * Returns a pseudo-random {@code int} value between inclusive {@code 0} and inclusive {@code range}.
     *
     * @throws IllegalArgumentException If {@code max - min + 1} is less than {@code 0}.
     */
    public static int inclusive(int range) {
        return inclusive(0, range);
    }

    /**
     * Pseudo-randomly retrieves an element from {@code array}.
     */
    public static <T> T random(T[] array) {
        return array[(int) (ThreadLocalRandom.current().nextDouble() * array.length)];
    }

    /**
     * Pseudo-randomly retrieves a {@code int} from {@code array}.
     */
    public static int random(int[] array) {
        return array[(int) (ThreadLocalRandom.current().nextDouble() * array.length)];
    }

    /**
     * Pseudo-randomly retrieves a {@code long} from {@code array}.
     */
    public static long random(long[] array) {
        return array[(int) (ThreadLocalRandom.current().nextDouble() * array.length)];
    }

    /**
     * Pseudo-randomly retrieves a {@code double} from {@code array}.
     */
    public static double random(double[] array) {
        return array[(int) (ThreadLocalRandom.current().nextDouble() * array.length)];
    }

    /**
     * Pseudo-randomly retrieves a {@code short} from {@code array}.
     */
    public static short random(short[] array) {
        return array[(int) (ThreadLocalRandom.current().nextDouble() * array.length)];
    }

    /**
     * Pseudo-randomly retrieves a {@code byte} from {@code array}.
     */
    public static byte random(byte[] array) {
        return array[(int) (ThreadLocalRandom.current().nextDouble() * array.length)];
    }

    /**
     * Pseudo-randomly retrieves a {@code float} from {@code array}.
     */
    public static float random(float[] array) {
        return array[(int) (ThreadLocalRandom.current().nextDouble() * array.length)];
    }

    /**
     * Pseudo-randomly retrieves a {@code boolean} from {@code array}.
     */
    public static boolean random(boolean[] array) {
        return array[(int) (ThreadLocalRandom.current().nextDouble() * array.length)];
    }

    /**
     * Pseudo-randomly retrieves a {@code char} from {@code array}.
     */
    public static char random(char[] array) {
        return array[(int) (ThreadLocalRandom.current().nextDouble() * array.length)];
    }

    /**
     * Pseudo-randomly retrieves a element from {@code list}.
     */
    public static <T> T random(List<T> list) {
        return list.get((int) (ThreadLocalRandom.current().nextDouble() * list.size()));
    }

    /**
     * Retrieves a random value from an {@code int} range.
     */
    public static int random(Range<Integer> range) {
        int low = range.hasLowerBound() ? range.lowerEndpoint() : Integer.MIN_VALUE;
        int high = range.hasUpperBound() ? range.upperEndpoint() : Integer.MAX_VALUE;
        if (range.upperBoundType() == BoundType.OPEN && range.lowerBoundType() == BoundType.CLOSED) {
            return inclusive(low - 1, high);
        } else if (range.upperBoundType() == BoundType.CLOSED && range.lowerBoundType() == BoundType.OPEN) {
            return inclusive(low, high - 1);
        } else if (range.upperBoundType() == BoundType.OPEN && range.lowerBoundType() == BoundType.OPEN) {
            return inclusive(low, high);
        } else if (range.upperBoundType() == BoundType.CLOSED && range.lowerBoundType() == BoundType.CLOSED) {
            return inclusive(low - 1, high - 1);
        }
        throw new Error("impossible");
    }

    /**
     * Shuffles the elements of a {@code Object} array.
     */
    public static <T> T[] shuffle(T[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = ThreadLocalRandom.current().nextInt(i + 1);
            T a = array[index];
            array[index] = array[i];
            array[i] = a;
        }
        return array;
    }

    /**
     * Shuffles the elements of a {@code int} array.
     */
    public static int[] shuffle(int[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = ThreadLocalRandom.current().nextInt(i + 1);
            int a = array[index];
            array[index] = array[i];
            array[i] = a;
        }
        return array;
    }

    /**
     * Shuffles the elements of a {@code long} array.
     */
    public static long[] shuffle(long[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = ThreadLocalRandom.current().nextInt(i + 1);
            long a = array[index];
            array[index] = array[i];
            array[i] = a;
        }
        return array;
    }

    /**
     * Shuffles the elements of a {@code double} array.
     */
    public static double[] shuffle(double[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = ThreadLocalRandom.current().nextInt(i + 1);
            double a = array[index];
            array[index] = array[i];
            array[i] = a;
        }
        return array;
    }

    /**
     * Shuffles the elements of a {@code short} array.
     */
    public static short[] shuffle(short[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = ThreadLocalRandom.current().nextInt(i + 1);
            short a = array[index];
            array[index] = array[i];
            array[i] = a;
        }
        return array;
    }

    /**
     * Shuffles the elements of a {@code byte} array.
     */
    public static byte[] shuffle(byte[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = ThreadLocalRandom.current().nextInt(i + 1);
            byte a = array[index];
            array[index] = array[i];
            array[i] = a;
        }
        return array;
    }

    /**
     * Shuffles the elements of a {@code float} array.
     */
    public static float[] shuffle(float[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = ThreadLocalRandom.current().nextInt(i + 1);
            float a = array[index];
            array[index] = array[i];
            array[i] = a;
        }
        return array;
    }

    /**
     * Shuffles the elements of a {@code boolean} array.
     */
    public static boolean[] shuffle(boolean[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = ThreadLocalRandom.current().nextInt(i + 1);
            boolean a = array[index];
            array[index] = array[i];
            array[i] = a;
        }
        return array;
    }

    /**
     * Shuffles the elements of a {@code char} array.
     */
    public static char[] shuffle(char[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = ThreadLocalRandom.current().nextInt(i + 1);
            char a = array[index];
            array[index] = array[i];
            array[i] = a;
        }
        return array;
    }

    /**
     * A private constructor to discourage external instantiation.
     */
    private RandomUtils() {
    }
}
