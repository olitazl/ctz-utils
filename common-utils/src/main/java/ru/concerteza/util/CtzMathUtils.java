package ru.concerteza.util;

import javax.annotation.Nullable;

/**
 * User: alexey
 * Date: 10/31/11
 */
public class CtzMathUtils {
    public static final int DEFAULT_INT = -1;

    public static int defaultInt(@Nullable Object input) {
        return defaultInt(input, DEFAULT_INT);
    }

    public static int defaultInt(@Nullable Object input, int defaultValue) {
        return null == input ? defaultValue : (Integer) input;
    }
}