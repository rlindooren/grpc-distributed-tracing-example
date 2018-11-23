package nl.javadev.grpc.tracing.util;

import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class TracingUtil {

    /**
     * Creates a new child span around the provided callable
     */
    public static <V> Callable<V> wrapInNewChildSpan(final Span parentSpan,
                                                     final String nameForTheChildSpan,
                                                     final Callable<V> callable) {

        return () -> Tracing.getTracer()
                .spanBuilderWithExplicitParent(nameForTheChildSpan, parentSpan)
                .startSpanAndCall(callable);
    }

    /**
     * Creates a new child span around the provided callable, using the current active span as the parent
     */
    public static <V> Callable<V> wrapInNewChildSpan(final String nameForTheChildSpan,
                                                     final Callable<V> callable) {
        return () -> Tracing.getTracer()
                .spanBuilder(nameForTheChildSpan)
                .startSpanAndCall(callable);
    }


    /**
     * Creates a new child span around the provided callable
     * and executes the callable asynchronously
     */
    public static <V> CompletableFuture<V> executeAsyncInNewChildSpan(final Span parentSpan,
                                                                      final String nameForTheChildSpan,
                                                                      final Callable<V> callable) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return wrapInNewChildSpan(parentSpan, nameForTheChildSpan, callable).call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    public static Span startNewParentSamplingSpan(final String spanName, final boolean makeItTheActiveSpan) {
        final var tracer = Tracing.getTracer();

        final var span = tracer.spanBuilderWithExplicitParent(spanName, null)
                .setSampler(Samplers.alwaysSample())
                .setRecordEvents(true)
                .startSpan();

        if (makeItTheActiveSpan) {
            tracer.withSpan(span);
        }

        return span;
    }

    public static Span createNewChildSpan(final String spanName, final Span parentSpan) {
        return Tracing.getTracer()
                .spanBuilderWithExplicitParent(spanName, parentSpan)
                .startSpan();
    }

}
