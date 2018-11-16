import io.grpc.ServerBuilder;
import nl.javadev.grpc.tracing.example.PriceService;

public class RunPriceServiceServer extends AbstractServerRunner {

    public static void main(String[] args) throws Exception {
        runServer(
                ServerBuilder.forPort(PRICE_SERVICE_PORT)
                        .addService(new PriceService())
                        .build()
        );
    }
}
