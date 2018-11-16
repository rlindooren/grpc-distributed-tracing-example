package nl.javadev.grpc.tracing.example;

import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import nl.javadev.grpc.tracing.example.GatewayServiceGrpc.GatewayServiceImplBase;
import nl.javadev.grpc.tracing.example.GatewayServiceOuterClass.GetProductsRequest;
import nl.javadev.grpc.tracing.example.GatewayServiceOuterClass.GetProductsResponse;
import nl.javadev.grpc.tracing.example.ProductServiceGrpc.ProductServiceBlockingStub;
import nl.javadev.grpc.tracing.example.ProductServiceOuterClass.GetProductsWithPriceAndStockDetailsRequest;

/**
 * The Gateway service simply does a blocking call to the Product service.
 * This is to demonstrate that in such a case the tracing span will be propagated to the next service automatically.
 */
public class GatewayService extends GatewayServiceImplBase {

    private final ProductServiceBlockingStub productServiceClient;

    public GatewayService(final String productServiceHost, final int productServicePort) {
        final var channel = ManagedChannelBuilder.forAddress(productServiceHost, productServicePort)
                .usePlaintext().build();
        productServiceClient = ProductServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public void getProducts(final GetProductsRequest request,
                            final StreamObserver<GetProductsResponse> responseObserver) {
        try {
            // Simply pass on the requested identifiers to the request to the Product service
            final var productServiceRequest = GetProductsWithPriceAndStockDetailsRequest.newBuilder()
                    .addAllProductIds(request.getProductIdsList())
                    .build();


            final var productServiceResponse = productServiceClient
                    .getProductsWithPriceAndStockDetails(productServiceRequest);

            // And then simply pass the returned products from the Product service to this response
            final var response = GetProductsResponse.newBuilder()
                    .addAllProducts(productServiceResponse.getProductsList())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
