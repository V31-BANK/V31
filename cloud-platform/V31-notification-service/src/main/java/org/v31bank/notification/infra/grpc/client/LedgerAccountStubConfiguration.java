package org.v31bank.notification.infra.grpc.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

import org.v31bank.ledger.api.v1.LedgerAccountServiceGrpc;

/**
 * The stub this service calls the ledger through.
 * <p>
 * Built from the named channel rather than from an address, so where the ledger
 * lives is a deployment concern: {@code spring.grpc.client.channel.ledger.target}
 * points at a host in one environment and at a service name in another, and
 * nothing in the code changes.
 * <p>
 * The channel the factory returns already carries the interceptors the gRPC
 * starter registers — the deadline every call leaves with, and the request
 * identifier that keeps a trace whole across the hop.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Configuration(proxyBeanMethods = false)
public class LedgerAccountStubConfiguration {

    /**
     * Name of the channel, matching the key under
     * {@code spring.grpc.client.channel}.
     */
    static final String LEDGER_CHANNEL = "ledger";

    @Bean
    public LedgerAccountServiceGrpc.LedgerAccountServiceBlockingStub ledgerAccountStub(GrpcChannelFactory channels) {
        return LedgerAccountServiceGrpc.newBlockingStub(channels.createChannel(LEDGER_CHANNEL));
    }

}
