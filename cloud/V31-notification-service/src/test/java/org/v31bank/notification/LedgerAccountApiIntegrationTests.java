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

package org.v31bank.notification;

import java.util.Map;
import java.util.UUID;

import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import org.v31bank.core.constant.ApiHeaders;
import org.v31bank.ledger.api.v1.CreateLedgerAccountRequest;
import org.v31bank.ledger.api.v1.CreateLedgerAccountResponse;
import org.v31bank.ledger.api.v1.GetLedgerAccountRequest;
import org.v31bank.ledger.api.v1.GetLedgerAccountResponse;
import org.v31bank.ledger.api.v1.LedgerAccount;
import org.v31bank.ledger.api.v1.LedgerAccountServiceGrpc;
import org.v31bank.ledger.api.v1.LedgerAccountStatus;
import org.v31bank.ledger.api.v1.LedgerAccountType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the ledger account endpoints this service serves by calling the
 * ledger over gRPC.
 * <p>
 * The service is started and driven over HTTP as it would be in production; the ledger
 * behind it is an in-process gRPC server rather than the real one, so what is exercised
 * here is this service's half — the controller, the adapter, the client interceptors, and
 * the translation of a remote refusal into the platform's envelope — without a second
 * process having to be running for the build to pass.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LedgerAccountApiIntegrationTests {

	private static final String PATH = "/api/v1/ledger-accounts";

	private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT = new ParameterizedTypeReference<>() {
	};

	private static final UUID ACCOUNT_ID = UUID.fromString("019fbe70-0000-7000-8000-00000000ab01");

	/**
	 * Started as the class loads, so its port is known by the time the context asks for
	 * it. On a real port rather than in-process: that is the transport the service uses
	 * in production, and it is what the configured channel factory knows how to reach.
	 */
	private static final Server SERVER = startLedger();

	@DynamicPropertySource
	static void ledgerTarget(DynamicPropertyRegistry registry) {
		registry.add("spring.grpc.client.channel.ledger.target", () -> "static://localhost:" + SERVER.getPort());
	}

	private static Server startLedger() {
		try {
			return ServerBuilder.forPort(0)
				.addService(ServerInterceptors.intercept(new FakeLedger(), new RequestIdRecorder()))
				.build()
				.start();
		}
		catch (java.io.IOException ex) {
			throw new IllegalStateException("Could not start the stand-in ledger", ex);
		}
	}

	@LocalServerPort
	private int port;

	private RestTestClient client;

	@BeforeEach
	void setUp() {
		this.client = RestTestClient.bindToServer().baseUrl("http://localhost:" + this.port).build();
		FakeLedger.reset();
	}

	@AfterAll
	static void stopLedger() {
		SERVER.shutdownNow();
	}

	@Test
	void createsAnAccountThroughTheLedger() {
		Map<String, Object> body = this.client.post()
			.uri(PATH)
			.body(Map.of("code", "ACC-1", "name", "Cash", "type", "ASSET"))
			.exchange()
			.expectStatus()
			.isCreated()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
		assertThat(body).containsEntry("success", true).containsEntry("code", "OK");
		assertThat(data(body)).containsEntry("code", "ACC-1")
			.containsEntry("name", "Cash")
			.containsEntry("type", "ASSET")
			.containsEntry("id", ACCOUNT_ID.toString());
	}

	@Test
	void findsAnAccountTheLedgerHolds() {
		assertThat(data(get(PATH + "/" + ACCOUNT_ID))).containsEntry("code", "ACC-1");
	}

	/**
	 * The reason the ledger gave has to survive the hop. Reported as a {@code 500} the
	 * caller would retry a request that can never succeed; reported without the code it
	 * would have to read the message to find out what happened.
	 */
	@Test
	void passesOnTheReasonTheLedgerRefused() {
		FakeLedger.refuseWith(Status.ALREADY_EXISTS.withDescription("Code 'ACC-1' is already in use"), "CONFLICT");

		Map<String, Object> body = this.client.post()
			.uri(PATH)
			.body(Map.of("code", "ACC-1", "name", "Cash", "type", "ASSET"))
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.CONFLICT)
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();

		assertThat(body).containsEntry("success", false)
			.containsEntry("code", "CONFLICT")
			.containsEntry("message", "Code 'ACC-1' is already in use");
	}

	@Test
	void reportsAnAccountTheLedgerDoesNotHaveAsNotFound() {
		FakeLedger.refuseWith(Status.NOT_FOUND.withDescription("No ledger account exists with id " + ACCOUNT_ID),
				"NOT_FOUND");

		this.client.get().uri(PATH + "/" + ACCOUNT_ID).exchange().expectStatus().isNotFound();
	}

	/**
	 * A failure with no code on it did not come from a V31 service — it came from the
	 * transport, a proxy, or a deadline. Its description is diagnostic text written for
	 * whoever reads the logs, and must not be handed to the caller.
	 */
	@Test
	void saysNothingAboutAFailureTheLedgerDidNotChoose() {
		FakeLedger.refuseWith(Status.UNAVAILABLE.withDescription("io exception: connect to ledger-primary refused"),
				null);

		Map<String, Object> body = this.client.get()
			.uri(PATH + "/" + ACCOUNT_ID)
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();

		assertThat((String) body.get("message")).doesNotContain("io exception").doesNotContain("ledger-primary");
	}

	/**
	 * Refused here rather than at the ledger: a request that cannot succeed should not
	 * cost a network round trip, and the caller is told which field to fix.
	 */
	@Test
	void rejectsAnEmptyBodyWithoutCallingTheLedger() {
		Map<String, Object> body = this.client.post()
			.uri(PATH)
			.body(Map.of())
			.exchange()
			.expectStatus()
			.isBadRequest()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();

		assertThat(body).containsEntry("code", "VALIDATION_FAILED");
		assertThat(FakeLedger.calls).isZero();
	}

	/**
	 * The identifier the request arrived with has to reach the ledger, or the two halves
	 * of one request cannot be joined up in the logs.
	 */
	@Test
	void carriesTheRequestIdentifierOnToTheLedger() {
		this.client.post()
			.uri(PATH)
			.header(ApiHeaders.REQUEST_ID, "REQ-ACROSS-HOPS")
			.body(Map.of("code", "ACC-1", "name", "Cash", "type", "ASSET"))
			.exchange()
			.expectStatus()
			.isCreated();

		assertThat(FakeLedger.lastRequestId).isEqualTo("REQ-ACROSS-HOPS");
	}

	/**
	 * A request that arrived without one is still given one, so the call onward is not
	 * the start of a second trace.
	 */
	@Test
	void issuesAnIdentifierForARequestThatArrivedWithout() {
		this.client.post()
			.uri(PATH)
			.body(Map.of("code", "ACC-1", "name", "Cash", "type", "ASSET"))
			.exchange()
			.expectStatus()
			.isCreated();

		assertThat(FakeLedger.lastRequestId).isNotBlank();
		assertThat(UUID.fromString(FakeLedger.lastRequestId).version()).isEqualTo(7);
	}

	private Map<String, Object> get(String uri) {
		return this.client.get()
			.uri(uri)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> data(Map<String, ?> envelope) {
		return (Map<String, Object>) envelope.get("data");
	}

	/**
	 * Stands in for the ledger, answering as it would and recording what reached it.
	 */
	static class FakeLedger extends LedgerAccountServiceGrpc.LedgerAccountServiceImplBase {

		private static final Metadata.Key<String> ERROR_CODE = Metadata.Key.of("v31-error-code",
				Metadata.ASCII_STRING_MARSHALLER);

		static final Metadata.Key<String> REQUEST_ID = Metadata.Key.of("x-request-id",
				Metadata.ASCII_STRING_MARSHALLER);

		static volatile String lastRequestId;

		static volatile int calls;

		private static volatile Status refusal;

		private static volatile String refusalCode;

		static void reset() {
			lastRequestId = null;
			calls = 0;
			refusal = null;
			refusalCode = null;
		}

		static void refuseWith(Status status, String errorCode) {
			refusal = status;
			refusalCode = errorCode;
		}

		@Override
		public void createLedgerAccount(CreateLedgerAccountRequest request,
				StreamObserver<CreateLedgerAccountResponse> observer) {
			answer(observer,
					CreateLedgerAccountResponse.newBuilder()
						.setLedgerAccount(LedgerAccount.newBuilder()
							.setId(ACCOUNT_ID.toString())
							.setCode(request.getCode())
							.setName(request.getName())
							.setType(request.getType())
							.setStatus(LedgerAccountStatus.LEDGER_ACCOUNT_STATUS_ACTIVE))
						.build());
		}

		@Override
		public void getLedgerAccount(GetLedgerAccountRequest request,
				StreamObserver<GetLedgerAccountResponse> observer) {
			answer(observer,
					GetLedgerAccountResponse.newBuilder()
						.setLedgerAccount(LedgerAccount.newBuilder()
							.setId(request.getId())
							.setCode("ACC-1")
							.setName("Cash")
							.setType(LedgerAccountType.LEDGER_ACCOUNT_TYPE_ASSET)
							.setStatus(LedgerAccountStatus.LEDGER_ACCOUNT_STATUS_ACTIVE))
						.build());
		}

		private <T> void answer(StreamObserver<T> observer, T response) {
			calls++;
			if (refusal != null) {
				observer.onError(asException(refusal, refusalCode));
				return;
			}
			observer.onNext(response);
			observer.onCompleted();
		}

		private static StatusRuntimeException asException(Status status, String errorCode) {
			Metadata trailers = new Metadata();
			if (errorCode != null) {
				trailers.put(ERROR_CODE, errorCode);
			}
			return status.asRuntimeException(trailers);
		}

	}

	/**
	 * Records what the client interceptor put on the call. Reading the metadata on the
	 * far side is the only way to tell that the identifier was actually sent, rather than
	 * merely present on the near side.
	 */
	static class RequestIdRecorder implements ServerInterceptor {

		@Override
		public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
				ServerCallHandler<ReqT, RespT> next) {
			FakeLedger.lastRequestId = headers.get(FakeLedger.REQUEST_ID);
			return next.startCall(call, headers);
		}

	}

}
