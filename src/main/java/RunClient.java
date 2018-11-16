import io.grpc.*;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceConfiguration;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceExporter;
import io.opencensus.trace.Span;
import nl.javadev.grpc.tracing.example.*;
import nl.javadev.grpc.tracing.example.GatewayServiceGrpc.GatewayServiceBlockingStub;
import nl.javadev.grpc.tracing.example.GatewayServiceOuterClass.GetProductsRequest;

import java.util.UUID;
import java.util.stream.IntStream;

class RunClient {

    public static void main(String[] args) throws Exception {
        StackdriverTraceExporter.createAndRegister(StackdriverTraceConfiguration.builder().build());

        final var channel = ManagedChannelBuilder.forAddress(AbstractServerRunner.HOST, AbstractServerRunner.GATEWAY_SERVICE_PORT)
                .usePlaintext().build();
        final var gatewayServiceClient = GatewayServiceGrpc.newBlockingStub(channel);

        // Make two calls, the first call takes a bit more time which may be confusing in the trace graph
        IntStream.rangeClosed(1, 2).forEach(i -> {
            final var parentSpan = startSamplingSpan();

            requestProducts(gatewayServiceClient);

            parentSpan.end();
        });

        System.out.println("\nWaiting 10 seconds to give the Stackdriver trace exporter a chance to upload the tracing information");
        Thread.sleep(10_000);
    }

    private static void requestProducts(final GatewayServiceBlockingStub gatewayServiceClient) {
        System.out.println("\nGoing to request some products");

        final var request = GetProductsRequest.newBuilder()
                .addProductIds("1")
                .addProductIds("2")
                .addProductIds("3")
                .build();

        final var response = gatewayServiceClient.getProducts(request);

        System.out.println(String.format("\nReceived products:%n%s", response.getProductsList()));
    }

    private static Span startSamplingSpan() {
        final var spanName = String.format("tracing-demo-%s", UUID.randomUUID());
        final var makeSpanActive = true;
        return TracingUtil.startNewParentSamplingSpan(spanName, makeSpanActive);
    }
}
