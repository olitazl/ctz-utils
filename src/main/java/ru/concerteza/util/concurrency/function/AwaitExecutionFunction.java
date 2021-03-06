package ru.concerteza.util.concurrency.function;

import com.google.common.base.Function;
import org.apache.commons.lang.UnhandledException;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Guava function, calls {@link java.util.concurrent.Future#get()} and returns result
 *
 * @author alexey
 * Date: 7/7/12
 * @see Future
 */
public class AwaitExecutionFunction<T> implements Function<Future<T>, T> {
    /**
     * {@inheritDoc}
     */
    @Override
    public T apply(@Nullable Future<T> input) {
        checkNotNull(input, "Provided future is null");
        try {
            return input.get();
        } catch(InterruptedException e) {
            throw new UnhandledException(e);
        } catch(ExecutionException e) {
            throw new UnhandledException(e);
        }
    }
}
