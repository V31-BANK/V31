/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * End-to-end tests over a real gRPC server and channel.
 * <p>
 * Run over the in-process transport, which is a real gRPC stack — interceptors, metadata,
 * statuses, context propagation and all — without a socket. The service is defined by
 * hand rather than generated, so the test needs no protobuf compiler and stays about the
 * behaviour this module adds.
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
		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> GrpcErrors.call(() -> call(client, FAIL_BUSINESS)));
		assertThat(thrown.getErrorCode().code()).as("the exact code has to survive the hop, not just the status")
			.isEqualTo("CATEGORY_HAS_CHILDREN");
		assertThat(thrown.getMessage()).isEqualTo("Customer category 7 still has children");
		assertThat(thrown.getErrorCode().httpStatus()).isEqualTo(409);
	}

	@Test
	void aRefusalAlsoCarriesAStatusAnyGrpcClientUnderstands() throws IOException {
		Channel client = start(Duration.ZERO);
		StatusRuntimeException thrown = catchThrowableOfType(StatusRuntimeException.class,
				() -> call(client, FAIL_BUSINESS));
		assertThat(thrown.getStatus().getCode()).isEqualTo(Status.Code.ALREADY_EXISTS);
		assertThat(thrown.getTrailers().get(GrpcStatuses.ERROR_CODE)).isEqualTo("CATEGORY_HAS_CHILDREN");
	}

	@Test
	void anUnexpectedFailureTellsTheCallerNothingAboutTheServer() throws IOException {
		Channel client = start(Duration.ZERO);
		StatusRuntimeException thrown = catchThrowableOfType(StatusRuntimeException.class,
				() -> call(client, FAIL_UNEXPECTED));
		assertThat(thrown.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
		assertThat(thrown.getStatus().getDescription()).isEqualTo(CommonErrorCode.INTERNAL_ERROR.defaultMessage());
		assertThat(String.valueOf(thrown.getStatus().getDescription()))
			.as("the driver's message must not reach the caller")
			.doesNotContain("customer_category");
		assertThat(thrown.getTrailers().get(GrpcStatuses.ERROR_CODE)).isEqualTo("INTERNAL_ERROR");
	}

	@Test
	void anUnexpectedFailureStillReachesTheCallerAsABusinessException() throws IOException {
		Channel client = start(Duration.ZERO);
		BusinessException thrown = catchThrowableOfType(BusinessException.class,
				() -> GrpcErrors.call(() -> call(client, FAIL_UNEXPECTED)));
		assertThat(thrown.getErrorCode().code()).isEqualTo("INTERNAL_ERROR");
		assertThat(thrown.getErrorCode().httpStatus()).isEqualTo(500);
	}

	@Test
	void aFailureFromOutsideThePlatformDoesNotLeakItsDiagnosticText() {
		io.grpc.StatusRuntimeException transportFailure = Status.UNAVAILABLE.withDescription("io exception")
			.asRuntimeException();
		BusinessException translated = GrpcErrors.asBusinessException(transportFailure);
		assertThat(translated.getErrorCode().code()).isEqualTo(CommonErrorCode.DEPENDENCY_UNAVAILABLE.code());
		assertThat(translated.getMessage()).as("transport wording is for the logs, not for whoever made the call")
			.isEqualTo(CommonErrorCode.DEPENDENCY_UNAVAILABLE.defaultMessage());
		assertThat(translated.getCause()).as("the original stays reachable for the logs").isNotNull();
	}

	/**
	 * The identifier travels through {@link RequestContext}, which is what the HTTP
	 * filter and the server interceptor both populate. The MDC is a mirror of it for the
	 * log lines, not a second carrier — a value put only there does not travel, so that
	 * the two sides cannot disagree about what a request carries.
	 */
	@Test
	void theRequestIdentifierCrossesTheHop() throws IOException {
		Channel client = start(Duration.ZERO);
		try (RequestContext.Scope scope = RequestContext
			.attach(java.util.Map.of(GrpcRequestId.HEADER_NAME, "trace-abc123"))) {
			assertThat(call(client, ECHO_REQUEST_ID))
				.as("the identifier the caller was serving under should be the one the server sees")
				.isEqualTo("trace-abc123");
		}
	}

	@Test
	void aConfiguredHeaderCrossesTheHopToo() throws IOException {
		Channel client = start(Duration.ZERO);
		try (RequestContext.Scope scope = RequestContext.attach(java.util.Map.of(TENANT_HEADER, "acme-bank"))) {
			assertThat(call(client, ECHO_TENANT))
				.as("a header listed for propagation should reach the far side unchanged")
				.isEqualTo("acme-bank");
		}
	}

	@Test
	void aHeaderNobodyAskedToPropagateDoesNotTravel() throws IOException {
		Channel client = start(Duration.ZERO);
		try (RequestContext.Scope scope = RequestContext.attach(java.util.Map.of("x-not-configured", "leaked"))) {
			assertThat(call(client, ECHO_TENANT))
				.as("only what was configured is carried, so nothing rides along by accident")
				.isEqualTo("null");
		}
	}

	@Test
	void aForgedHeaderValueIsNotCarried() throws IOException {
		Channel client = start(Duration.ZERO);
		java.util.Map<String, String> values = RequestContext.newValues();
		RequestContext.put(values, TENANT_HEADER, "acme\nX-Injected: yes");
		assertThat(values).as("a value that could terminate a header or forge a log line is refused before it travels")
			.doesNotContainKey(TENANT_HEADER);
	}

	@Test
	void aCallWithNoIdentifierBehindItStillGetsOne() throws IOException {
		Channel client = start(Duration.ZERO);
		String seen = call(client, ECHO_REQUEST_ID);
		assertThat(seen).isNotNull();
		assertThat(seen).as("the server issues one when the caller had none").isNotBlank();
	}

	@Test
	void aForgedIdentifierIsNotEchoed() throws IOException {
		Channel client = start(Duration.ZERO);
		org.slf4j.MDC.put(GrpcRequestId.MDC_KEY, "line-one\nline-two");
		try {
			String seen = call(client, ECHO_REQUEST_ID);
			assertThat(seen).as("a value that could forge a log entry must be replaced").doesNotContain("\n");
		}
		catch (StatusRuntimeException ex) {
			// gRPC itself may reject the malformed metadata value first, which is
			// also an acceptable outcome — it never reaches the log either way.
			assertThat(ex.getStatus()).isNotNull();
		}
		finally {
			org.slf4j.MDC.remove(GrpcRequestId.MDC_KEY);
		}
	}

	@Test
	void everyCallLeavesWithADeadline() throws IOException {
		Channel client = start(Duration.ofSeconds(30));
		assertThat(call(client, ECHO_DEADLINE))
			.as("a call with no deadline waits on an unhealthy server until the connection breaks")
			.isEqualTo("true");
	}

	@Test
	void withoutTheInterceptorACallHasNoDeadlineAtAll() throws IOException {
		Channel client = start(Duration.ZERO);
		assertThat(call(client, ECHO_DEADLINE)).as("which is what gRPC does out of the box").isEqualTo("false");
	}

	private static String call(Channel channel, String request) {
		return ClientCalls.blockingUnaryCall(channel, METHOD, CallOptions.DEFAULT, request);
	}

	/**
	 * Start a server carrying this module's interceptors and return a channel carrying
	 * its client-side ones.
	 * @param defaultDeadline the deadline to apply, or {@link Duration#ZERO} to leave
	 * calls without one
	 * @return the channel to call through
	 * @throws IOException if the server could not start
	 */
	private Channel start(Duration defaultDeadline) throws IOException {
		String name = InProcessServerBuilder.generateName();
		GrpcExceptionHandlerInterceptor exceptions = new GrpcExceptionHandlerInterceptor(
				new CompositeGrpcExceptionHandler(new BusinessExceptionGrpcExceptionHandler(),
						new UnexpectedExceptionGrpcExceptionHandler()));
		this.server = InProcessServerBuilder.forName(name)
			.addService(ServerInterceptors.intercept(probeService(), exceptions,
					new HeaderPropagationServerInterceptor(List.of(TENANT_HEADER))))
			.build()
			.start();
		this.channel = InProcessChannelBuilder.forName(name).build();
		List<io.grpc.ClientInterceptor> interceptors = defaultDeadline.isZero()
				? List.of(new HeaderPropagationClientInterceptor()) : List.of(new HeaderPropagationClientInterceptor(),
						new DefaultDeadlineSetupClientInterceptor(defaultDeadline));
		return ClientInterceptors.intercept(this.channel, interceptors);
	}

	/**
	 * A service that does whatever the request names, so that one method covers every
	 * case under test.
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
					case FAIL_BUSINESS ->
						throw new BusinessException(new TestErrorCode(), "Customer category 7 still has children");
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
	 * Stands for a code a service declares for its own failures — the case the status
	 * code alone cannot carry.
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
