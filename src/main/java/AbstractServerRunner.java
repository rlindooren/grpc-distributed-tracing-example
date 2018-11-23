import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceConfiguration;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceExporter;
import nl.javadev.grpc.tracing.util.SampleRemoteSpanFactory;

import java.util.stream.Collectors;

class AbstractServerRunner {
    // Start all servers locally,
    final static String HOST = "localhost";

    // but listening on different ports
    final static int GATEWAY_SERVICE_PORT = 10_001;
    final static int PRODUCT_SERVICE_PORT = 10_002;
    final static int PRICE_SERVICE_PORT = 10_003;
    final static int STOCKLEVEL_SERVICE_PORT = 10_004;

    static void runServer(final ServerBuilder serverBuilder) throws Exception {
        // Start an exporter for every server instance
        StackdriverTraceExporter.createAndRegister(StackdriverTraceConfiguration.builder().build());

        // Register a stream tracer factory that will listen for incoming sampling spans
        serverBuilder.addStreamTracerFactory(new SampleRemoteSpanFactory());

        final Server server = serverBuilder.build();

        // Stop the server when the JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            System.out.println(String.format("Stopped server on port: %s for service: %s",
                    server.getPort(), getServiceNamesFromServer(server)));
        }));

        server.start();
        System.out.println(String.format("Started server on port: %s for service: %s",
                server.getPort(), getServiceNamesFromServer(server)));

        System.out.println("Press [ENTER] to stop this server");
        System.in.read();
    }

    private static String getServiceNamesFromServer(Server server) {
        return server.getImmutableServices().stream()
                .map(ServerServiceDefinition::getServiceDescriptor)
                .map(ServiceDescriptor::getName)
                .collect(Collectors.joining());
    }
}
