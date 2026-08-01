package org.v31bank.grpc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.grpc.client.interceptor.DefaultDeadlineSetupClientInterceptor;
import org.springframework.grpc.server.exception.CompositeGrpcExceptionHandler;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;

import org.v31bank.core.exception.BusinessException;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.core.response.ErrorCode;
import org.v31bank.grpc.client.GrpcErrors;
import org.v31bank.grpc.client.HeaderPropagationClientInterceptor;
import org.v31bank.grpc.context.GrpcRequestId;
import org.v31bank.grpc.context.RequestContext;
import org.v31bank.grpc.server.BusinessExceptionGrpcExceptionHandler;
import org.v31bank.grpc.server.HeaderPropagationServerInterceptor;
import org.v31bank.grpc.server.UnexpectedExceptionGrpcExceptionHandler;
import org.v31bank.grpc.status.GrpcStatuses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests over a real gRPC server and channel.
 * <p>
 * Run over the in-process transport, which is a real gRPC stack — interceptors,
 * metadata, statuses, context propagation and all — without a socket. The service
 * is defined by hand rather than generated, so the test needs no protobuf
 * compiler and stays about the behaviour this module adds.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class GrpcEndToEndTests {

    /**
     * What the handler is asked to do, sent as the request.
     */
    private static final String ECHO_REQUEST_ID = "echo-request-id";

    private static final String ECHO_DEADLINE = "echo-deadline";

    private static final String ECHO_TENANT = "echo-tenant";

    private static final String TENANT_HEADER = "x-tenant-id";

    private static final String FAIL_BUSINESS = "fail-business";

    private static final String FAIL_UNEXPECTED = "fail-unexpected";

    private static final MethodDescriptor.Marshaller<String> STRINGS = new MethodDescriptor.Marshaller<>() {

        @Override
        public InputStream stream(String value) {
            return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String parse(InputStream stream) {
            try {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

    };

    private static final MethodDescriptor<String, String> METHOD = MethodDescriptor.<String, String>newBuilder()
        .setType(MethodDescriptor.MethodType.UNARY)
        .setFullMethodName("v31.Probe/Call")
        .setRequestMarshaller(STRINGS)
        .setResponseMarshaller(STRINGS)
        .build();

    private Server server;

    private ManagedChannel channel;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (this.channel != null) {
            this.channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
        if (this.server != null) {
            this.server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void aRefusalArrivesCarryingTheCodeTheServerThrew() throws IOException {
        Channel client = start(Duration.ZERO);
        BusinessException thrown = assertThrows(BusinessException.class, () -> GrpcErrors.call(() -> call(client,
                FAIL_BUSINESS)));
        assertEquals("CATEGORY_HAS_CHILDREN", thrown.getErrorCode().code(),
                "the exact code has to survive the hop, not just the status");
        assertEquals("Customer category 7 still has children", thrown.getMessage());
        assertEquals(409, thrown.getErrorCode().httpStatus());
    }

    @Test
    void aRefusalAlsoCarriesAStatusAnyGrpcClientUnderstands() throws IOException {
        Channel client = start(Duration.ZERO);
        StatusRuntimeException thrown = assertThrows(StatusRuntimeException.class, () -> call(client, FAIL_BUSINESS));
        assertEquals(Status.Code.ALREADY_EXISTS, thrown.getStatus().getCode());
        assertEquals("CATEGORY_HAS_CHILDREN", thrown.getTrailers().get(GrpcStatuses.ERROR_CODE));
    }

    @Test
    void anUnexpectedFailureTellsTheCallerNothingAboutTheServer() throws IOException {
        Channel client = start(Duration.ZERO);
        StatusRuntimeException thrown = assertThrows(StatusRuntimeException.class, () -> call(client, FAIL_UNEXPECTED));
        assertEquals(Status.Code.INTERNAL, thrown.getStatus().getCode());
        assertEquals(CommonErrorCode.INTERNAL_ERROR.defaultMessage(), thrown.getStatus().getDescription());
        assertFalse(String.valueOf(thrown.getStatus().getDescription()).contains("customer_category"),
                "the driver's message must not reach the caller");
        assertEquals("INTERNAL_ERROR", thrown.getTrailers().get(GrpcStatuses.ERROR_CODE));
    }

    @Test
    void anUnexpectedFailureStillReachesTheCallerAsABusinessException() throws IOException {
        Channel client = start(Duration.ZERO);
        BusinessException thrown = assertThrows(BusinessException.class,
                () -> GrpcErrors.call(() -> call(client, FAIL_UNEXPECTED)));
        assertEquals("INTERNAL_ERROR", thrown.getErrorCode().code());
        assertEquals(500, thrown.getErrorCode().httpStatus());
    }

    @Test
    void aFailureFromOutsideThePlatformDoesNotLeakItsDiagnosticText() {
        io.grpc.StatusRuntimeException transportFailure = Status.UNAVAILABLE.withDescription("io exception")
            .asRuntimeException();
        BusinessException translated = GrpcErrors.asBusinessException(transportFailure);
        assertEquals(CommonErrorCode.DEPENDENCY_UNAVAILABLE.code(), translated.getErrorCode().code());
        assertEquals(CommonErrorCode.DEPENDENCY_UNAVAILABLE.defaultMessage(), translated.getMessage(),
                "transport wording is for the logs, not for whoever made the call");
        assertNotNull(translated.getCause(), "the original stays reachable for the logs");
    }

    /**
     * The identifier travels through {@link RequestContext}, which is what the
     * HTTP filter and the server interceptor both populate. The MDC is a mirror of
     * it for the log lines, not a second carrier — a value put only there does not
     * travel, so that the two sides cannot disagree about what a request carries.
     */
    @Test
    void theRequestIdentifierCrossesTheHop() throws IOException {
        Channel client = start(Duration.ZERO);
        try (RequestContext.Scope scope = RequestContext
            .attach(java.util.Map.of(GrpcRequestId.HEADER_NAME, "trace-abc123"))) {
            assertEquals("trace-abc123", call(client, ECHO_REQUEST_ID),
                    "the identifier the caller was serving under should be the one the server sees");
        }
    }

    @Test
    void aConfiguredHeaderCrossesTheHopToo() throws IOException {
        Channel client = start(Duration.ZERO);
        try (RequestContext.Scope scope = RequestContext.attach(java.util.Map.of(TENANT_HEADER, "acme-bank"))) {
            assertEquals("acme-bank", call(client, ECHO_TENANT),
                    "a header listed for propagation should reach the far side unchanged");
        }
    }

    @Test
    void aHeaderNobodyAskedToPropagateDoesNotTravel() throws IOException {
        Channel client = start(Duration.ZERO);
        try (RequestContext.Scope scope = RequestContext.attach(java.util.Map.of("x-not-configured", "leaked"))) {
            assertEquals("null", call(client, ECHO_TENANT),
                    "only what was configured is carried, so nothing rides along by accident");
        }
    }

    @Test
    void aForgedHeaderValueIsNotCarried() throws IOException {
        Channel client = start(Duration.ZERO);
        java.util.Map<String, String> values = RequestContext.newValues();
        RequestContext.put(values, TENANT_HEADER, "acme\nX-Injected: yes");
        assertFalse(values.containsKey(TENANT_HEADER),
                "a value that could terminate a header or forge a log line is refused before it travels");
    }

    @Test
    void aCallWithNoIdentifierBehindItStillGetsOne() throws IOException {
        Channel client = start(Duration.ZERO);
        String seen = call(client, ECHO_REQUEST_ID);
        assertNotNull(seen);
        assertFalse(seen.isBlank(), "the server issues one when the caller had none");
    }

    @Test
    void aForgedIdentifierIsNotEchoed() throws IOException {
        Channel client = start(Duration.ZERO);
        org.slf4j.MDC.put(GrpcRequestId.MDC_KEY, "line-one\nline-two");
        try {
            String seen = call(client, ECHO_REQUEST_ID);
            assertFalse(seen.contains("\n"), "a value that could forge a log entry must be replaced");
        }
        catch (StatusRuntimeException ex) {
            // gRPC itself may reject the malformed metadata value first, which is
            // also an acceptable outcome — it never reaches the log either way.
            assertNotNull(ex.getStatus());
        }
        finally {
            org.slf4j.MDC.remove(GrpcRequestId.MDC_KEY);
        }
    }

    @Test
    void everyCallLeavesWithADeadline() throws IOException {
        Channel client = start(Duration.ofSeconds(30));
        assertEquals("true", call(client, ECHO_DEADLINE),
                "a call with no deadline waits on an unhealthy server until the connection breaks");
    }

    @Test
    void withoutTheInterceptorACallHasNoDeadlineAtAll() throws IOException {
        Channel client = start(Duration.ZERO);
        assertEquals("false", call(client, ECHO_DEADLINE), "which is what gRPC does out of the box");
    }

    private static String call(Channel channel, String request) {
        return ClientCalls.blockingUnaryCall(channel, METHOD, CallOptions.DEFAULT, request);
    }

    /**
     * Start a server carrying this module's interceptors and return a channel
     * carrying its client-side ones.
     * @param defaultDeadline the deadline to apply, or {@link Duration#ZERO} to
     * leave calls without one
     * @return the channel to call through
     * @throws IOException if the server could not start
     */
    private Channel start(Duration defaultDeadline) throws IOException {
        String name = InProcessServerBuilder.generateName();
        GrpcExceptionHandlerInterceptor exceptions = new GrpcExceptionHandlerInterceptor(
                new CompositeGrpcExceptionHandler(new BusinessExceptionGrpcExceptionHandler(),
                        new UnexpectedExceptionGrpcExceptionHandler()));
        this.server = InProcessServerBuilder.forName(name)
            .addService(ServerInterceptors.intercept(probeService(), exceptions, new HeaderPropagationServerInterceptor(List.of(TENANT_HEADER))))
            .build()
            .start();
        this.channel = InProcessChannelBuilder.forName(name).build();
        List<io.grpc.ClientInterceptor> interceptors = defaultDeadline.isZero()
                ? List.of(new HeaderPropagationClientInterceptor())
                : List.of(new HeaderPropagationClientInterceptor(), new DefaultDeadlineSetupClientInterceptor(defaultDeadline));
        return ClientInterceptors.intercept(this.channel, interceptors);
    }

    /**
     * A service that does whatever the request names, so that one method covers
     * every case under test.
     * @return the service definition
     */
    private static ServerServiceDefinition probeService() {
        return ServerServiceDefinition.builder("v31.Probe")
            .addMethod(METHOD, ServerCalls.asyncUnaryCall((String request, StreamObserver<String> observer) -> {
                switch (request) {
                    case ECHO_REQUEST_ID -> {
                        observer.onNext(String.valueOf(GrpcRequestId.CONTEXT_KEY.get()));
                        observer.onCompleted();
                    }
                    case ECHO_TENANT -> {
                        observer.onNext(String.valueOf(RequestContext.get(TENANT_HEADER)));
                        observer.onCompleted();
                    }
                    case ECHO_DEADLINE -> {
                        observer.onNext(String.valueOf(Context.current().getDeadline() != null));
                        observer.onCompleted();
                    }
                    case FAIL_BUSINESS -> throw new BusinessException(new TestErrorCode(),
                            "Customer category 7 still has children");
                    case FAIL_UNEXPECTED ->
                        throw new IllegalStateException("ERROR: relation \"customer_category\" does not exist");
                    default -> {
                        observer.onNext(request);
                        observer.onCompleted();
                    }
                }
            }))
            .build();
    }

    /**
     * Stands for a code a service declares for its own failures — the case the
     * status code alone cannot carry.
     */
    private record TestErrorCode() implements ErrorCode {

        @Override
        public String code() {
            return "CATEGORY_HAS_CHILDREN";
        }

        @Override
        public String defaultMessage() {
            return "That category still has children";
        }

        @Override
        public int httpStatus() {
            return 409;
        }

    }

}
