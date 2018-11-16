package nl.javadev.grpc.tracing.example;

import io.grpc.stub.StreamObserver;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;
import io.vavr.concurrent.Future;
import io.vavr.control.Option;
import nl.javadev.grpc.tracing.example.PriceServiceGrpc.PriceServiceImplBase;
import nl.javadev.grpc.tracing.example.PriceServiceOuterClass.GetPriceForProductRequest;
import nl.javadev.grpc.tracing.example.PriceServiceOuterClass.GetPriceForProductResponse;

import java.math.BigDecimal;
import java.util.Random;

/**
 * The price service retrieves the price from a legacy price information system.
 * It does this in a non-blocking manner using VAVR.
 */
public class PriceService extends PriceServiceImplBase {

    private final Random random = new Random();

    @Override
    public void getPriceForProduct(final GetPriceForProductRequest request,
                                   final StreamObserver<GetPriceForProductResponse> responseObserver) {

        // Capture the span bound to the GRPC thread that calls this method.
        // It will not be available for the threads that execute the asynchronous logic.
        final var span = Tracing.getTracer().getCurrentSpan();

        getPriceFromPriceInformationSystem(request.getProductId(), span)
                .map(this::mapOptionalPriceToResponse)
                .onSuccess(response -> {
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                })
                .onFailure(responseObserver::onError);
    }

    private GetPriceForProductResponse mapOptionalPriceToResponse(final Option<BigDecimal> optionalPrice) {
        final var responseBuilder = GetPriceForProductResponse.newBuilder()
                .setCouldBeDetermined(optionalPrice.isDefined());
        optionalPrice.forEach(price -> responseBuilder.setPrice(price.doubleValue()));
        return responseBuilder.build();
    }

    /**
     * This method fakes a call to a third party system
     */
    private Future<Option<BigDecimal>> getPriceFromPriceInformationSystem(final String productId, final Span span) {

        // TODO span

        return Future
                // Add some latency
                .runRunnable(() -> SleepUtil.sleepRandomly(20))
                // Come up with a random price
                .map(aVoid -> new BigDecimal(String.format("%s.%s",
                        random.nextInt(5) + 1,
                        random.nextInt(100))))
                // Wrap it in an option
                // it's meaningless for this code, but in a real system the requested product might not have price information
                .map(Option::of);
    }
}
