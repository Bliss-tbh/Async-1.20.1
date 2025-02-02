package com.axalotl.async.parallelised.thread;

public class Assertions {

    public static void assertTrue(boolean value, String message) {
        if (!value) {
            final AssertionError error = new AssertionError(message);
            error.printStackTrace();
            throw error;
        }
    }
}
