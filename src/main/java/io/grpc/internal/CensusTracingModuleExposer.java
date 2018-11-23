package io.grpc.internal;

import io.grpc.Metadata;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracing;

/**
 * This class exposes the GRPC OpenCensus tracing module.
 */
public class CensusTracingModuleExposer {
    private static CensusTracingModule INSTANCE;

    public static Metadata.Key<SpanContext> getTracingHeader() {
        return getCensusTracingModuleInstance().tracingHeader;
    }

    private static synchronized CensusTracingModule getCensusTracingModuleInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CensusTracingModule(Tracing.getTracer(), Tracing.getPropagationComponent().getBinaryFormat());
        }

        return INSTANCE;
    }
}
