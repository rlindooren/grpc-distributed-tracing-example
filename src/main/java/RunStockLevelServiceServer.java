import io.grpc.ServerBuilder;
import nl.javadev.grpc.tracing.example.StockLevelService;

public class RunStockLevelServiceServer extends AbstractServerRunner {

    public static void main(String[] args) throws Exception {
        runServer(
                ServerBuilder.forPort(STOCKLEVEL_SERVICE_PORT)
                        .addService(new StockLevelService())
                        .build()
        );
    }
}
