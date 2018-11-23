import io.grpc.ServerBuilder;
import nl.javadev.grpc.tracing.example.GatewayService;

public class RunGatewayServiceServer extends AbstractServerRunner {

    public static void main(String[] args) throws Exception {
        runServer(
                ServerBuilder.forPort(GATEWAY_SERVICE_PORT)
                        .addService(new GatewayService(HOST, PRODUCT_SERVICE_PORT))
        );
    }
}
