package nl.javadev.grpc.tracing.util;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerStreamTracer;
import io.grpc.Status;
import io.grpc.internal.CensusTracingModuleExposer;
import io.opencensus.trace.Span;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;
import io.opencensus.trace.unsafe.ContextUtils;

/**
 * This stream tracer will detect if a rpc call is done with a sampling span.
 * If so then we continue this remote span.
 */
public class SampleRemoteSpanTracer extends ServerStreamTracer {
    private Span sampledSpan = null;

    SampleRemoteSpanTracer(String fullMethodName, Metadata headers) {
        SpanContext remoteSpanContext = headers.get(CensusTracingModuleExposer.getTracingHeader());
        if (remoteSpanContext == SpanContext.INVALID) {
            remoteSpanContext = null;
        }

        if (remoteSpanContext != null && remoteSpanContext.getTraceOptions().isSampled()) {
            System.out.println(
                    String.format("Found a sampled remote span/trace for call to '%s': %s. Going to create a sampling child span",
                            fullMethodName, remoteSpanContext));

            sampledSpan =
                    Tracing.getTracer()
                            .spanBuilderWithRemoteParent(fullMethodName, remoteSpanContext)
                            .setSampler(Samplers.alwaysSample())
                            .startSpan();
        } else {
            System.out.println(String.format("No sampled remote span/trace found: %s", remoteSpanContext));
        }
    }

    public Context filterContext(Context context) {
        return this.sampledSpan != null ? context.withValue(ContextUtils.CONTEXT_SPAN_KEY, this.sampledSpan) : context;
    }

    public void streamClosed(Status status) {
        if (this.sampledSpan != null) {
            this.sampledSpan.end();
        }
    }
}
