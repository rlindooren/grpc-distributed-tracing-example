package nl.javadev.grpc.tracing.util;

import io.grpc.Metadata;
import io.grpc.ServerStreamTracer;

public class SampleRemoteSpanFactory extends ServerStreamTracer.Factory {

    public ServerStreamTracer newServerStreamTracer(String fullMethodName, Metadata headers) {
        return new SampleRemoteSpanTracer(fullMethodName, headers);
    }

}
