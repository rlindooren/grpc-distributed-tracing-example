import io.grpc.ServerBuilder;
import nl.javadev.grpc.tracing.example.ProductService;

public class RunProductServiceServer extends AbstractServerRunner {

    public static void main(String[] args) throws Exception {
        runServer(
                ServerBuilder.forPort(PRODUCT_SERVICE_PORT)
                        .addService(new ProductService(HOST, PRICE_SERVICE_PORT, HOST, STOCKLEVEL_SERVICE_PORT))
                        .build()
        );
    }
}
