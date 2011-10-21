package ru.concerteza.util;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static ru.concerteza.util.CtzFormatUtils.format;

/**
 * User: alexey
 * Date: 6/9/11
 */
public class CtzCollectionUtils {
    // Lists
    public static <F, T> List<T> transformCopy(List<F> fromList, Function<? super F, ? extends T> function) {
        return ImmutableList.copyOf(Lists.transform(fromList, function));
    }

    public static <T> List<T> filterCopy(final List<T> unfiltered, final Predicate<? super T> predicate) {
        return ImmutableList.copyOf(Iterables.filter(unfiltered, predicate));
    }

    public static <T> List<T> filterCopy(final List<?> unfiltered, final Class<T> type) {
        return ImmutableList.copyOf(Iterables.filter(unfiltered, type));
    }

    // Sets
    public static <F, T> Set<T> transformCopy(Set<F> fromSet, Function<? super F, ? extends T> function) {
        return ImmutableSet.copyOf(Collections2.transform(fromSet, function));
    }

    public static <T> Set<T> filterCopy(final Set<T> unfiltered, final Predicate<? super T> predicate) {
        return ImmutableSet.copyOf(Iterables.filter(unfiltered, predicate));
    }

    public static <T> Set<T> filterCopy(final Set<?> unfiltered, final Class<T> type) {
        return ImmutableSet.copyOf(Iterables.filter(unfiltered, type));
    }
}
