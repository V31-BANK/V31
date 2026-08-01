package org.v31bank.grpc.autoconfigure;

import io.grpc.ServerInterceptor;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;

import org.v31bank.grpc.server.BusinessExceptionGrpcExceptionHandler;
import org.v31bank.grpc.server.HeaderPropagationServerInterceptor;
import org.v31bank.grpc.server.UnexpectedExceptionGrpcExceptionHandler;

/**
 * {@link AutoConfiguration Auto-configuration} for the calls a V31 service
 * serves: reports refusals with the platform's own error codes, keeps the detail
 * of an unexpected failure off the wire, and gives every call an identifier.
 * <p>
 * Spring gRPC already gathers every {@link GrpcExceptionHandler} bean into one
 * interceptor and asks them in order, so this contributes handlers rather than
 * wiring an interceptor of its own — a service adds a handler for its own
 * failures the same way, and the ones here stay out of its way.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@AutoConfiguration(before = GrpcServerAutoConfiguration.class)
@ConditionalOnClass({ ServerInterceptor.class, GrpcExceptionHandler.class })
@EnableConfigurationProperties(V31GrpcProperties.class)
public class V31GrpcServerAutoConfiguration {

    /**
     * Translates a refusal the caller is meant to see.
     * @return the handler
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBooleanProperty(name = "v31.grpc.server.exception-handling.enabled", matchIfMissing = true)
    public BusinessExceptionGrpcExceptionHandler businessExceptionGrpcExceptionHandler() {
        return new BusinessExceptionGrpcExceptionHandler();
    }

    /**
     * Answers everything else, last, without disclosing why.
     * @return the handler
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBooleanProperty(name = "v31.grpc.server.exception-handling.enabled", matchIfMissing = true)
    public UnexpectedExceptionGrpcExceptionHandler unexpectedExceptionGrpcExceptionHandler() {
        return new UnexpectedExceptionGrpcExceptionHandler();
    }

    /**
     * Takes what a call arrived carrying and makes it reachable for the rest of
     * it, including for the calls made onward.
     * @param properties the header names to carry beyond the request identifier
     * @return the interceptor
     */
    @Bean
    @GlobalServerInterceptor
    @ConditionalOnMissingBean
    @ConditionalOnBooleanProperty(name = "v31.grpc.propagation.enabled", matchIfMissing = true)
    public HeaderPropagationServerInterceptor headerPropagationServerInterceptor(V31GrpcProperties properties) {
        return new HeaderPropagationServerInterceptor(properties.getPropagation().getHeaders());
    }

}
