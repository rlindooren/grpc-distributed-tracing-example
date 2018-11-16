package nl.javadev.grpc.tracing.example;

import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;
import io.vavr.concurrent.Future;
import io.vavr.control.Option;
import lombok.Value;
import nl.javadev.grpc.tracing.example.PriceServiceGrpc.PriceServiceFutureStub;
import nl.javadev.grpc.tracing.example.PriceServiceOuterClass.GetPriceForProductRequest;
import nl.javadev.grpc.tracing.example.PriceServiceOuterClass.GetPriceForProductResponse;
import nl.javadev.grpc.tracing.example.ProductServiceOuterClass.*;
import nl.javadev.grpc.tracing.example.StockLevelServiceGrpc.StockLevelServiceFutureStub;
import nl.javadev.grpc.tracing.example.StockLevelServiceOuterClass.GetCurrentStockLevelForProductRequest;
import nl.javadev.grpc.tracing.example.StockLevelServiceOuterClass.GetCurrentStockLevelForProductResponse;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The product service looks up a product data in the database and enriches this with price and stock information from other services.
 * It does so in an asynchronous way (using VAVR).
 * This code demonstrates that we then explicitly have to capture and pass on the span to continue it.
 */
public class ProductService extends ProductServiceGrpc.ProductServiceImplBase {

    private final PriceServiceFutureStub priceServiceClient;
    private final StockLevelServiceFutureStub stockLevelServiceClient;

    public ProductService(final String priceServiceHost, final int priceServicePort,
                          final String stockLevelServiceHost, final int stockLevelServicePort) {

        final var priceServiceChannel = ManagedChannelBuilder.forAddress(priceServiceHost, priceServicePort)
                .usePlaintext().build();
        priceServiceClient = PriceServiceGrpc.newFutureStub(priceServiceChannel);

        final var stockLevelServiceChannel = ManagedChannelBuilder.forAddress(stockLevelServiceHost, stockLevelServicePort)
                .usePlaintext().build();
        stockLevelServiceClient = StockLevelServiceGrpc.newFutureStub(stockLevelServiceChannel);
    }

    @Override
    public void getProductsWithPriceAndStockDetails(final GetProductsWithPriceAndStockDetailsRequest request,
                                                    final StreamObserver<GetProductsWithPriceAndStockDetailsResponse> responseObserver) {
        // Capture the span bound to the GRPC thread that calls this method.
        // It will not be available for the threads that execute the asynchronous steps.
        final var span = Tracing.getTracer().getCurrentSpan();

        Future.sequence(createFutureForEachProduct(request.getProductIdsList(), span))
                .map(productsWithDetails ->
                        GetProductsWithPriceAndStockDetailsResponse.newBuilder()
                                .addAllProducts(productsWithDetails)
                                .build())
                .onSuccess(response -> {
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                })
                .onFailure(responseObserver::onError);
    }

    /**
     * This method fakes retrieving a product from the database.
     * In a real system this would probably go through a DAO layer or via a different micro service.
     */
    private Future<Option<BasicProductData>> getProductFromDatabase(final String productId, final Span span) {

        final var spanName = String.format("getProductFromDatabase:%s", productId);

        return Future
                .fromCompletableFuture(
                        // This callable will be executed asynchronously within a new span
                        TracingUtil.executeAsyncInNewChildSpan(span, spanName,
                                () -> {
                                    // Add some latency
                                    SleepUtil.sleepRandomly(20);
                                    // Come up with a some simple product data
                                    return new BasicProductData(productId, String.format("Description for product %s", productId));
                                })
                )
                // Wrap it in an option
                // it's meaningless for this code, but in a real system the requested product might not exist
                .map(Option::of);
    }

    /**
     * This method calls the Price service
     */
    private Future<Double> getPriceForProduct(final String productId, final Span span) {
        // Explicitly set the span as the active span on the current thread
        // so the client call will pick it up and pass it on
        Tracing.getTracer().withSpan(span);

        final var priceForProductRequest = GetPriceForProductRequest.newBuilder()
                .setProductId(productId)
                .build();

        return Future
                .fromJavaFuture(priceServiceClient.getPriceForProduct(priceForProductRequest))
                .map(GetPriceForProductResponse::getPrice);
    }

    /**
     * This method calls the Stock level service
     */
    private Future<Integer> getStockLevelForProduct(final String productId, final Span span) {
        // Explicitly set the span as the active span on the current thread
        // so the client call will pick it up and pass it on
        Tracing.getTracer().withSpan(span);

        final var stockLevelForProductRequest = GetCurrentStockLevelForProductRequest.newBuilder()
                .setProductId(productId)
                .build();

        return Future
                .fromJavaFuture(stockLevelServiceClient.getCurrentStockLevelForProduct(stockLevelForProductRequest))
                .map(GetCurrentStockLevelForProductResponse::getStockLevel);
    }

    private List<Future<ProductWithDetails>> createFutureForEachProduct(final List<String> productIds,
                                                                        final Span span) {
        return productIds.stream()
                .map(productId -> getProductAndDetails(productId, span))
                .collect(Collectors.toList());
    }

    private Future<ProductWithDetails> getProductAndDetails(final String productId, final Span span) {
        final var spanName = String.format("getProductAndDetails:%s", productId);
        return Future
                .successful(TracingUtil.createNewChildSpan(spanName, span))
                .flatMap(childSpan ->
                        // Start by retrieving the basic product data from the database
                        getProductFromDatabase(productId, childSpan)
                                // Fail if the product could not be found in the database
                                .map(optionalProduct ->
                                        optionalProduct.getOrElseThrow(() ->
                                                new RuntimeException(String.format("Product %s not found in database", productId)))
                                )
                                // Create the ProductWithDetails, but without price and stock details filled in
                                .map(product ->
                                        ProductWithDetails.newBuilder()
                                                .setId(product.id)
                                                .setDescription(product.description)
                                                .build()
                                )
                                // Fill in the price and stock details asynchronously
                                .flatMap(pwd -> enrichProductWithPriceAndStockDetails(pwd, childSpan))
                                .onComplete(productWithDetails -> childSpan.end())
                );
    }

    private Future<ProductWithDetails> enrichProductWithPriceAndStockDetails(final ProductWithDetails productWithDetails,
                                                                             final Span span) {
        return Future.fold(
                List.of(
                        // Retrieve the price
                        getPriceForProduct(productWithDetails.getId(), span)
                                // Map to product details, but only set the price details
                                .map(price -> ProductWithDetails.newBuilder()
                                        .setPriceDetails(PriceDetails.newBuilder().setCurrentPrice(price))
                                        .build()),
                        // Retrieve the stock level
                        getStockLevelForProduct(productWithDetails.getId(), span)
                                // Map to product details, but only set the stock details
                                .map(stock -> ProductWithDetails.newBuilder()
                                        .setStockDetails(StockDetails.newBuilder().setNrInStock(stock))
                                        .build())
                ),
                // Merge price and stock details
                productWithDetails, this::mergePriceAndStockDetails
        );
    }

    private ProductWithDetails mergePriceAndStockDetails(final ProductWithDetails original,
                                                         final ProductWithDetails other) {
        final var builder = original.toBuilder();

        // If the price details have not explicitly been set, then use the price from the other
        if (builder.getPriceDetails().equals(PriceDetails.getDefaultInstance())) {
            builder.setPriceDetails(other.getPriceDetails());
        }

        // If the stock details have not explicitly been set, then use the stock from the other
        if (builder.getStockDetails().equals(StockDetails.getDefaultInstance())) {
            builder.setStockDetails(other.getStockDetails());
        }

        return builder.build();
    }

    @Value
    private class BasicProductData {
        private String id;
        private String description;
    }
}
