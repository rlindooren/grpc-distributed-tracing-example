package nl.javadev.grpc.tracing.example;

import java.util.Random;

class SleepUtil {

    final static private Random random = new Random();

    /**
     * Sleep at least the given amount of milliseconds plus an additional amount of milliseconds (0 - 300)
     */
    public static void sleepRandomly(final long atLeastMillis) {
        try {
            Thread.sleep(atLeastMillis + random.nextInt(301));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
