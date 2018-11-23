package nl.javadev.grpc.tracing.example;

import io.grpc.stub.StreamObserver;
import nl.javadev.grpc.tracing.example.StockLevelServiceGrpc.StockLevelServiceImplBase;
import nl.javadev.grpc.tracing.example.StockLevelServiceOuterClass.GetCurrentStockLevelForProductRequest;
import nl.javadev.grpc.tracing.example.StockLevelServiceOuterClass.GetCurrentStockLevelForProductResponse;
import nl.javadev.grpc.tracing.util.TracingUtil;

import java.util.Optional;
import java.util.Random;

/**
 * The StockLevel service retrieves the stock level from a legacy Warehousing system.
 * It does this in a blocking manner.
 */
public class StockLevelService extends StockLevelServiceImplBase {

    @Override
    public void getCurrentStockLevelForProduct(final GetCurrentStockLevelForProductRequest request,
                                               final StreamObserver<GetCurrentStockLevelForProductResponse> responseObserver) {

        try {
            final var optionalLevel = getStockLevelFromWarehouseManagementSystem(request.getProductId());

            final var responseBuilder = GetCurrentStockLevelForProductResponse.newBuilder()
                    .setCouldBeDetermined(optionalLevel.isPresent());

            optionalLevel.ifPresent(responseBuilder::setStockLevel);

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(ex);
        }
    }

    /**
     * This method fakes a call to a third party system
     */
    private Optional<Integer> getStockLevelFromWarehouseManagementSystem(final String productId) throws Exception {
        return TracingUtil.wrapInNewChildSpan("getStockLevelFromWarehouseManagementSystem", () -> {
                    SleepUtil.sleepRandomly(20);
                    // Wrap it in an option
                    // it's meaningless for this code,
                    // but in a real system the requested product might not have stock information
                    return Optional.of(new Random().nextInt(500));
                }
        ).call();
    }
}
